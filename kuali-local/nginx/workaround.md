## "Connection Refused" issue workaround

A "Connection Refused" issue comes up when containers try to contact other services through their REST apis.

EXAMPLES: user authentication and JWT validation. 

Such requests are issued by a container *(not the browser)*, going through the reverse-proxy to the container of the target service. This routing comes with a "Connection Refused" error. The issue only affects local hosting setups whose inter-service traffic goes over localhost or 127.0.0.1.
There probably is a standard configuration setting available that can account for the local hosting environment but what it is remains elusive.
The issue manifests as follows *(during a call to the kc identity service api):*

```
org.kuali.coeus.sys.impl.auth.JwtServiceImpl.verifyToken(String jwtString, String secret) >
   org.jose4j.jwt.consumer.JwtConsumer.processContext >
      org.jose4j.jws.JsonWebSignature.verifySignature >
         org.jose4j.jws.HmacUsingShaAlgorithm.validateKey ... throws a InvalidKeyException
```

where "secret" is the value set in kc-config.xml for `"auth.filter.service2service.secret"`.
This means that the secret used in producing the key in the request JWT cannot be verified by using the secret again to produce another key that satisfies the comparison of being the same byte length. This indicates that the original secret used may have been something different (or null?).

##### Cause:

`auth.base.url` or `app.host` from kc-config.xml is the default prefix for /api/vi/... api calls to core or other services. However, these will fail in a local development setup with connection refused *(even with correct JWT in header)* because they are based on localhost. Localhost urls, when issued from within a container get caught up in loopback and go nowhere. Attempts will produce:

```
Failed to connect to ::1: Cannot assign requested address
```

Use of 127.0.0.1 has a similar effect:

```
Failed connect to 127.0.0.1:443; Connection refused
```

There probably is a standard configuration setting available that can account for the local hosting environment but what it is remains elusive.
For now it will suffice to use a workaround approach for this.
Several environment variables applied to containers are modified to specify these direct links:

> *http://[service name]:[container port]*

- Kuali-research: CORE_PRIVATE_URL=http://cor-main:3000
- Kuali-research: KC_PRIVATE_HOST=kuali-research:8080
- Kuali-research: PDF_PRIVATE_URL=http://research-pdf:3006
- Research-portal: CORE_AUTH_BASE_URL=http://cor-main:3000
- Research-pdf: AUTH_BASEURL=http://cor-main:3000

These target the related service directly with http *(not https)* over the docker network bridge on the specific port the associated container is listening on. This bypasses the reverse proxy via the outer localhost and avoids the issue.

## Dashboard Routing issue workaround

The "Connection Refused" workaround comes with a side-effect: the configuration variable set to identify this non-proxied service address is also used by the research-portal app which "bakes" it into some of the browser routing links it creates which are not reachable since the docker network bridge is mostly an exclusive containers-only club. These wind up as hyperlinks rendered into dashboard html pages and redirects, causing a sort of reverse problem as the one we are trying to solve. So, restore proper navigation:

- The reverse proxy is extended with instructions to identify the related errant request and send back redirects to the equivalent location known to the reverse proxy. 

- A system host file entry is made making it possible to implement this solution by providing a host matching the errant requests so they can reach the reverse proxy for redirection. Put the following entry in your systems host file:

  ```
  127.0.0.1 cor-main
  ```

  

