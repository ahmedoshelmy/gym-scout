// One container app, scaled to zero when idle. Single replica: the app keeps no shared state outside Postgres,
// but the Caffeine route cache is per instance, so more replicas only cost more upstream calls.
param name string
param location string
param environmentId string
param image string
param targetPort int
param external bool

param secrets array
param env array

resource app 'Microsoft.App/containerApps@2024-03-01' = {
  name: name
  location: location
  properties: {
    managedEnvironmentId: environmentId
    configuration: {
      ingress: {
        external: external
        targetPort: targetPort
        transport: 'auto'
      }
      secrets: secrets
    }
    template: {
      containers: [
        {
          name: name
          image: image
          env: env
          resources: { cpu: json('0.5'), memory: '1Gi' }
        }
      ]
      scale: { minReplicas: 0, maxReplicas: 1 }
    }
  }
}

output url string = 'https://${app.properties.configuration.ingress.fqdn}'
