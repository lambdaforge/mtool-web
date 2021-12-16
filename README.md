# mtool-web

generated using Luminus version "3.91" with the following options:

    lein new luminus mtool +reitit +datomic +cljs +re-frame +shadow-cljs +swagger +auth-jwe +kibit
    
In addition, it is configured with Datahike and by default Bulma css is included.


## Prerequisites

You will need [Leiningen][1] 2.0 or above installed.

[1]: https://github.com/technomancy/leiningen

## Running

To start a web server for the application, run:

    lein run
    
To start shadow-cljs

    lein shadow watch app


## Docker and Docker Compose

### Build and run image 
 
 * Create image
 
 `docker build -t mtool:[version] .`

 **Version:** Version tag. Ex: **0.0.1** or **latest**

 * Run image
 
 `docker run -d -p 3000:3000 [version tag]`

### Docker-Compose

To run docker-compose it is necessary to add a .env file with the postgres credentials

`$ cat .env`

```
# Change password on production environment

P_USER = mtool_user
P_PASSWD = change-me
```

 * First time (Build image)

 `docker-compose up --build`

 * After the image is built

 `docker-compose up -d`


## License

Copyright © 2021 FIXME
