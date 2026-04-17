FROM mcr.microsoft.com/dotnet/aspnet:9.0 AS base
WORKDIR /app
EXPOSE 10000

FROM mcr.microsoft.com/dotnet/sdk:9.0 AS build
WORKDIR /src
COPY ["SmartFlashcardAPI.csproj", "."]
RUN dotnet restore
COPY . .
RUN dotnet publish -c Release -o /app/publish

FROM base AS final
WORKDIR /app
COPY --from=build /app/publish .

# Create wwwroot directory for generated images
RUN mkdir -p /app/wwwroot/generated_images

ENV ASPNETCORE_URLS=http://+:10000
ENTRYPOINT ["dotnet", "SmartFlashcardAPI.dll"]
