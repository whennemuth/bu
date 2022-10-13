const defer = require('config/defer').deferConfig
const team = 'cor'
const product = 'main'
const environment = 'default'

module.exports = {
  team,
  product,
  environment,
  app: {
    name: `${team}-${product}-${environment}`
  },
  port: process.env.PORT || 3000,
  serviceSecret: [
    process.env.SERVICE_SECRET_1 || 'kc.service.secret.kc.service.secret.kc.service.secret',
    process.env.SERVICE_SECRET_2 || 'kc.service.secret.kc.service.secret.kc.service.secret'
  ].filter(Boolean),
  baseUrl: 'http://localhost:3000',
  seneca: { options: {} },
  log: {
                name: defer(cfg => cfg.app.name),
                team,
                product,
                environment,
                format: 'pretty',
                level: 'debug',
                src: false
  },
  rootDomain: '.kuali.co',
  cookieOpts: {
    httpOnly: false,
    secure: false,
    domain: '',
    path: '/',
    maxAge: 1209600000
  },
  db: {
    uri: process.env.MONGO_URI || 'mongodb://${CORE_HOST}/core-development',
    options: null
  },
  elasticsearch: {
    host: null,
    operationWaitTime: 1000
  },
  cache: {
    silentFail: true,
    host: process.env.REDIS_URI || 'localhost',
    port: process.env.REDIS_PORT || 6379
  },
  resourceLock: {
    keyPrefix: 'lock:',
    ttl: 60
  },
  auth: {
    preAuthSecret: 'changeme',
    casCallbackUrl: 'http://${CORE_HOST}:3000',
    samlCallbackUrl: 'https://saml1-tst.kuali.co/auth/saml/consume',
    samlIssuerUrl: 'https://saml1-tst.kuali.co/auth',
    samlMeta: './samlMeta.xml',
    samlMeta2: './samlMeta2.xml',
    samlMetaCA: './samlMetaCA.xml',
    samlMeta2Template: './samlMeta2Template.txt',
    samlKeys: {
      v0:
        process.env.SAML_KEY_V0 ||
        'Paste the default value here',
      v2:
        process.env.SAML_KEY_V2 ||
        'Paste the default value here'
    },
    acceptedClockSkewMs: -1,
    maxApiTokensPerUser: 25
  },
  nonUpdateableUsernames: [],
  nodeAppInstanceId: process.pid
}
