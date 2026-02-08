FROM nginx:1.25-alpine

LABEL maintainer="SaveYourMoney Team"
LABEL description="SaveYourMoney Dashboard UI"

# Copy static files
COPY public/ /usr/share/nginx/html/

# Custom nginx config for SPA routing
RUN echo 'server { \
    listen 8080; \
    root /usr/share/nginx/html; \
    index index.html; \
    \
    location / { \
        try_files $uri $uri/ /index.html; \
    } \
    \
    location /api/ { \
        proxy_pass http://analyzer-service:8084/; \
        proxy_set_header Host $host; \
        proxy_set_header X-Real-IP $remote_addr; \
    } \
    \
    gzip on; \
    gzip_types text/plain text/css application/json application/javascript text/xml; \
    gzip_min_length 1024; \
}' > /etc/nginx/conf.d/default.conf

EXPOSE 8080

CMD ["nginx", "-g", "daemon off;"]
