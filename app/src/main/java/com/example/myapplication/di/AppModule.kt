package com.example.myapplication.di

import android.content.Context
import androidx.room.Room
import com.example.myapplication.data.local.AppDatabase
import com.example.myapplication.data.local.DatabaseMigrations
import com.example.myapplication.data.local.dao.AiChatDao
import com.example.myapplication.data.local.dao.DeckDao
import com.example.myapplication.data.local.dao.FlashcardDao
import com.example.myapplication.data.local.dao.ReviewLogDao
import com.example.myapplication.data.local.dao.UserDao
import com.example.myapplication.data.remote.AuthInterceptor
import com.example.myapplication.data.remote.NetworkConfig
import com.example.myapplication.data.remote.TokenManager
import com.example.myapplication.data.remote.TokenRefreshInterceptor
import com.example.myapplication.data.remote.api.AdminApi
import com.example.myapplication.data.remote.api.AiApi
import com.example.myapplication.data.remote.api.AuthApi
import com.example.myapplication.data.remote.api.FlashcardApi
import com.example.myapplication.data.remote.api.ShareApi
import com.example.myapplication.data.remote.api.SharedImageApi
import com.example.myapplication.data.remote.api.SyncApi
import com.example.myapplication.data.repository.AdminRepositoryImpl
import com.example.myapplication.data.repository.AiRepositoryImpl
import com.example.myapplication.data.repository.DeckRepositoryImpl
import com.example.myapplication.data.repository.FlashcardRepositoryImpl
import com.example.myapplication.data.repository.ShareRepositoryImpl
import com.example.myapplication.data.repository.UserRepositoryImpl
import com.example.myapplication.domain.repository.AdminRepository
import com.example.myapplication.domain.repository.AiRepository
import com.example.myapplication.domain.repository.DeckRepository
import com.example.myapplication.domain.repository.FlashcardRepository
import com.example.myapplication.domain.repository.UserRepository
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    // ════════════════════════════════════════
    //  CONFIGURATION
    // ════════════════════════════════════════

    // TODO: Đổi NetworkConfig.BASE_URL theo môi trường:
    //   Emulator  → "http://10.0.2.2:5000/"
    //   Thiết bị thật (cùng WiFi) → "http://192.168.x.x:5000/"
    //   Production → "https://your-api-domain.com/"

    // ════════════════════════════════════════
    //  NETWORK
    // ════════════════════════════════════════

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = true
    }

    @Provides
    @Singleton
    fun provideTokenManager(@ApplicationContext context: Context): TokenManager =
        TokenManager(context)

    /** No interceptors — used by [TokenRefreshInterceptor] to avoid Retrofit/OkHttp cycles. */
    @Provides
    @Singleton
    @Named("plain")
    fun providePlainOkHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor,
        tokenRefreshInterceptor: TokenRefreshInterceptor
    ): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(tokenRefreshInterceptor)
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                }
            )
            .connectTimeout(120, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .build()

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, json: Json): Retrofit =
        Retrofit.Builder()
            .baseUrl(NetworkConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi =
        retrofit.create(AuthApi::class.java)

    @Provides
    @Singleton
    fun provideFlashcardApi(retrofit: Retrofit): FlashcardApi =
        retrofit.create(FlashcardApi::class.java)

    @Provides
    @Singleton
    fun provideAiApi(retrofit: Retrofit): AiApi =
        retrofit.create(AiApi::class.java)

    @Provides
    @Singleton
    fun provideSyncApi(retrofit: Retrofit): SyncApi =
        retrofit.create(SyncApi::class.java)

    @Provides
    @Singleton
    fun provideAdminApi(retrofit: Retrofit): AdminApi =
        retrofit.create(AdminApi::class.java)

    @Provides
    @Singleton
    fun provideShareApi(retrofit: Retrofit): ShareApi =
        retrofit.create(ShareApi::class.java)

    @Provides
    @Singleton
    fun provideSharedImageApi(retrofit: Retrofit): SharedImageApi =
        retrofit.create(SharedImageApi::class.java)

    // ════════════════════════════════════════
    //  DATABASE
    // ════════════════════════════════════════

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
            .addMigrations(DatabaseMigrations.MIGRATION_1_2)
            .addMigrations(DatabaseMigrations.MIGRATION_2_3)
            .addMigrations(DatabaseMigrations.MIGRATION_3_4)
            .build()

    @Provides
    fun provideUserDao(db: AppDatabase): UserDao = db.userDao()

    @Provides
    fun provideDeckDao(db: AppDatabase): DeckDao = db.deckDao()

    @Provides
    fun provideFlashcardDao(db: AppDatabase): FlashcardDao = db.flashcardDao()

    @Provides
    fun provideReviewLogDao(db: AppDatabase): ReviewLogDao = db.reviewLogDao()

    @Provides
    fun provideAiChatDao(db: AppDatabase): AiChatDao = db.aiChatDao()

    // ════════════════════════════════════════
    //  REPOSITORIES
    // ════════════════════════════════════════

    @Provides
    @Singleton
    fun provideDeckRepository(
        deckDao: DeckDao,
        flashcardDao: FlashcardDao,
        flashcardApi: FlashcardApi
    ): DeckRepository = DeckRepositoryImpl(deckDao, flashcardDao, flashcardApi)

    @Provides
    @Singleton
    fun provideFlashcardRepository(
        flashcardDao: FlashcardDao,
        flashcardApi: FlashcardApi
    ): FlashcardRepository = FlashcardRepositoryImpl(flashcardDao, flashcardApi)

    @Provides
    @Singleton
    fun provideAiRepository(aiApi: AiApi): AiRepository =
        AiRepositoryImpl(aiApi)

    @Provides
    @Singleton
    fun provideAdminRepository(adminApi: AdminApi): AdminRepository =
        AdminRepositoryImpl(adminApi)

    @Provides
    @Singleton
    fun provideShareRepository(shareApi: ShareApi): ShareRepositoryImpl =
        ShareRepositoryImpl(shareApi)

    @Provides
    @Singleton
    fun provideUserRepository(
        authApi: AuthApi,
        userDao: UserDao,
        tokenManager: TokenManager,
        deckRepository: DeckRepository,
        reviewLogRepository: com.example.myapplication.domain.repository.ReviewLogRepository
    ): UserRepository = UserRepositoryImpl(authApi, userDao, tokenManager, deckRepository, reviewLogRepository)

    @Provides
    @Singleton
    fun provideReviewLogRepository(
        reviewLogDao: com.example.myapplication.data.local.dao.ReviewLogDao,
        flashcardApi: FlashcardApi
    ): com.example.myapplication.domain.repository.ReviewLogRepository =
        com.example.myapplication.data.repository.ReviewLogRepositoryImpl(reviewLogDao, flashcardApi)

    @Provides
    @Singleton
    fun provideSyncRepository(syncManager: com.example.myapplication.data.repository.SyncManager): com.example.myapplication.domain.repository.SyncRepository =
        syncManager
}
