@description('Azure region for every resource')
param location string = resourceGroup().location

@description('Short prefix used in resource names')
@minLength(3)
@maxLength(12)
param prefix string = 'notsureance'

@description('App Service plan size')
@allowed(['B1', 'B2', 'P0v3', 'P1v3'])
param planSku string = 'B1'

@description('PostgreSQL administrator login')
param databaseAdmin string = 'claimsadmin'

@secure()
@description('PostgreSQL administrator password')
param databasePassword string

@description('Entra tenant that signs user tokens')
param entraTenantId string

@description('Accepted token audiences, comma separated (API app id and api:// URI)')
param entraApiAudiences string

@description('Client id of the Angular app registration')
param entraSpaClientId string

@description('API scopes the Angular app asks for, comma separated')
param entraApiScopes string

@description('Default domain of the tenant, used to map the demo users')
param demoEmailDomain string = 'notsureance.dev'

@description('Bolt URI of the Neo4j graph, for example an AuraDB instance. Leave empty to switch the graph off.')
param neo4jUri string = ''

@description('Neo4j user name')
param neo4jUsername string = 'neo4j'

@secure()
@description('Neo4j password')
param neo4jPassword string = ''

@description('Load the demo farms, claims and accounts. Keep false for real environments.')
param seedDemoData bool = false

var suffix = uniqueString(resourceGroup().id)
var registryName = toLower('${prefix}${suffix}')
var appName = '${prefix}-claims-${suffix}'
var databaseServerName = '${prefix}-pg-${suffix}'
var acrPullRoleId = subscriptionResourceId('Microsoft.Authorization/roleDefinitions', '7f951dda-4ed3-4680-a7ca-43fe172d538d')
var flywayLocations = seedDemoData ? 'classpath:db/migration,classpath:db/demo' : 'classpath:db/migration'

resource registry 'Microsoft.ContainerRegistry/registries@2023-07-01' = {
  name: registryName
  location: location
  sku: {
    name: 'Basic'
  }
  properties: {
    adminUserEnabled: false
  }
}

resource database 'Microsoft.DBforPostgreSQL/flexibleServers@2022-12-01' = {
  name: databaseServerName
  location: location
  sku: {
    name: 'Standard_B1ms'
    tier: 'Burstable'
  }
  properties: {
    version: '16'
    administratorLogin: databaseAdmin
    administratorLoginPassword: databasePassword
    storage: {
      storageSizeGB: 32
    }
    backup: {
      backupRetentionDays: 7
      geoRedundantBackup: 'Disabled'
    }
    highAvailability: {
      mode: 'Disabled'
    }
  }
}

resource claimsDatabase 'Microsoft.DBforPostgreSQL/flexibleServers/databases@2022-12-01' = {
  parent: database
  name: 'notsureance'
}

resource allowAzureServices 'Microsoft.DBforPostgreSQL/flexibleServers/firewallRules@2022-12-01' = {
  parent: database
  name: 'AllowAzureServices'
  properties: {
    startIpAddress: '0.0.0.0'
    endIpAddress: '0.0.0.0'
  }
}

resource plan 'Microsoft.Web/serverfarms@2023-12-01' = {
  name: '${prefix}-plan-${suffix}'
  location: location
  kind: 'linux'
  sku: {
    name: planSku
  }
  properties: {
    reserved: true
  }
}

resource app 'Microsoft.Web/sites@2023-12-01' = {
  name: appName
  location: location
  kind: 'app,linux,container'
  identity: {
    type: 'SystemAssigned'
  }
  properties: {
    serverFarmId: plan.id
    httpsOnly: true
    siteConfig: {
      linuxFxVersion: 'DOCKER|${registry.properties.loginServer}/notsureance-claims:latest'
      acrUseManagedIdentityCreds: true
      alwaysOn: true
      http20Enabled: true
      minTlsVersion: '1.2'
      ftpsState: 'Disabled'
      healthCheckPath: '/actuator/health/readiness'
      appSettings: [
        { name: 'WEBSITES_PORT', value: '8080' }
        { name: 'DATABASE_URL', value: 'jdbc:postgresql://${database.properties.fullyQualifiedDomainName}:5432/notsureance?sslmode=require' }
        { name: 'DATABASE_USER', value: databaseAdmin }
        { name: 'DATABASE_PASSWORD', value: databasePassword }
        { name: 'ENTRA_TENANT_ID', value: entraTenantId }
        { name: 'ENTRA_API_AUDIENCES', value: entraApiAudiences }
        { name: 'ENTRA_SPA_CLIENT_ID', value: entraSpaClientId }
        { name: 'ENTRA_API_SCOPES', value: entraApiScopes }
        { name: 'DEMO_EMAIL_DOMAIN', value: demoEmailDomain }
        { name: 'ALLOWED_ORIGINS', value: 'https://${appName}.azurewebsites.net' }
        { name: 'FLYWAY_LOCATIONS', value: flywayLocations }
        { name: 'GRAPH_ENABLED', value: empty(neo4jUri) ? 'false' : 'true' }
        { name: 'NEO4J_URI', value: neo4jUri }
        { name: 'NEO4J_USERNAME', value: neo4jUsername }
        { name: 'NEO4J_PASSWORD', value: neo4jPassword }
      ]
    }
  }
}

resource acrPull 'Microsoft.Authorization/roleAssignments@2022-04-01' = {
  name: guid(registry.id, app.id, acrPullRoleId)
  scope: registry
  properties: {
    principalId: app.identity.principalId
    principalType: 'ServicePrincipal'
    roleDefinitionId: acrPullRoleId
  }
}

output webAppName string = app.name
output registryName string = registry.name
output url string = 'https://${app.properties.defaultHostName}'
