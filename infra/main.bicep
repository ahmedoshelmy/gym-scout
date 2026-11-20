// Gym Scout on Azure: Container Apps (consumption) for the API and the web proxy, Postgres Flexible Server
// with the PostGIS extension allow-listed. Optional and manual, nothing in CI deploys:
//   az deployment group create -g <rg> -f infra/main.bicep -p postgresAdminPassword=<secret>
targetScope = 'resourceGroup'

@description('Prefix for resource names.')
param name string = 'gymscout'

param location string = resourceGroup().location

@description('Registry and tag of the images you built with `docker compose build` and pushed.')
param imageRegistry string = 'ghcr.io/example'
param imageTag string = 'latest'

param postgresAdminLogin string = 'gymscoutadmin'

@secure()
param postgresAdminPassword string

@description('Zone used for "today" (visits, weekly volume, cost per visit).')
param zone string = 'Africa/Cairo'

module env 'modules/environment.bicep' = {
  name: 'environment'
  params: {
    name: name
    location: location
  }
}

module postgres 'modules/postgres.bicep' = {
  name: 'postgres'
  params: {
    name: '${name}-pg-${uniqueString(resourceGroup().id)}'
    location: location
    adminLogin: postgresAdminLogin
    adminPassword: postgresAdminPassword
    databaseName: 'gymscout'
  }
}

// The web container's nginx proxies /api to http://api:8080, so the API app must be called "api".
module api 'modules/containerapp.bicep' = {
  name: 'app-api'
  params: {
    name: 'api'
    location: location
    environmentId: env.outputs.environmentId
    image: '${imageRegistry}/gym-scout-api:${imageTag}'
    targetPort: 8080
    external: false
    secrets: [
      { name: 'db-password', value: postgresAdminPassword }
    ]
    env: [
      { name: 'DATABASE_URL', value: 'jdbc:postgresql://${postgres.outputs.fqdn}:5432/gymscout?sslmode=require' }
      { name: 'DATABASE_USER', value: postgresAdminLogin }
      { name: 'DATABASE_PASSWORD', secretRef: 'db-password' }
      { name: 'GYMSCOUT_ZONE', value: zone }
      { name: 'GYMSCOUT_DEMO_DATA', value: 'true' }
    ]
  }
}

module web 'modules/containerapp.bicep' = {
  name: 'app-web'
  params: {
    name: '${name}-web'
    location: location
    environmentId: env.outputs.environmentId
    image: '${imageRegistry}/gym-scout-web:${imageTag}'
    targetPort: 80
    external: true
    secrets: []
    env: []
  }
}

output webUrl string = web.outputs.url
