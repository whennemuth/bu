# FROM nginx:alpine

FROM nginx

RUN \
  mkdir -p /usr/lib/mycerts && \
  cd /usr/lib/mycerts && \
  openssl req \
     -newkey rsa:4096 -nodes -sha256 -keyout domain.key \
     -x509 -days 365 -out domain.crt

# COPY nginx.conf /etc/nginx/nginx.conf


