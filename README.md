<p align="left">
    <img alt="Datahike" src="https://raw.githubusercontent.com/replikativ/datahike/main/doc/assets/datahike-logo.svg" height="128em">
</p>

[![CI](https://github.com/alekcz/datahike-jdbc-server/actions/workflows/main.yml/badge.svg)](https://github.com/alekcz/datahike-jdbc-server/actions/workflows/main.yml) [![codecov](https://codecov.io/gh/alekcz/datahike-jdbc-server/branch/main/graph/badge.svg?token=UkLQlpnfbp)](https://codecov.io/gh/alekcz/datahike-jdbc-server)   

## Deploy JDBC Server

[![Deploy to DO](https://www.deploytodo.com/do-btn-blue.svg)](https://cloud.digitalocean.com/apps/new?repo=https://github.com/alekcz/datahike-jdbc-server/tree/main&refcode=a0cfd79e40a2)  

We both get credits for DigitalOcean if you end using their services so be a mate.   

Or you could deploy to Heroku  

[![Deploy](https://www.herokucdn.com/deploy/button.svg)](https://heroku.com/deploy?template=https://github.com/alekcz/datahike-jdbc-server/tree/main)

## Build Datahike JDBC Server

`clj -T:build uber` builds an uberjar into the `target/`-directory.

## Run Datahike JDBC Server

Run datahike-jdbc-server in locally from the source (requires docker and docker-compse):

`bash bin/start.sh` (requires docker and docker-compse)

Or you could run the docker image like so (you'd need postgresql running):

```bash 
docker run \
  --env DATAHIKE_JDBC_NAME=mydb \
  --env DATAHIKE_JDBC_DBTYPE=postgresql \
  --env DATAHIKE_JDBC_HOST=localhost \
  --env DATAHIKE_JDBC_DBNAME=datahike \
  --env DATAHIKE_JDBC_USER=datahike \
  --env DATAHIKE_JDBC_PASSWORD=password \
  --env DATAHIKE_JDBC_PORT=4000 \
  --env DATAHIKE_JDBC_TOKEN=foshizzle \
  --env DATAHIKE_JDBC_DEV_MODE=true \
  -p 4000:4000 \
  alekcz/datahike-jdbc-server:latest 
```

## Configuring Datahike JDBC Server
### File Configuration

Firetomic loads configuration from `resources/config.edn` relative to the
current directory. This file has a number of options and overwrites all other
configuration given via environment or properties. Below you can find an example
to configure both Datahike and the server.
```
{:databases [{:store {:backend :jdbc}
              :schema-flexibility :read
              :keep-history? false
              :name "sessions"}
             {:store {:backend :jdbc}
              :name "users"
              :keep-history? true
              :schema-flexibility :write}]
 :server {:port  3333
          :loglevel :info
          :dbtype "postgresql"
          :host "localhost"
          :dbname "datahike"
          :user "datahike"
          :password "password"
          :dev-mode false
          :token :securerandompassword}}
```

### Configuration via Environment and Properties

Datahike JDBC Server can also be configured via environment variables. 
Please take a look at the [configuration of Datahike](https://github.com/replikativ/datahike/blob/development/doc/config.md) to get an
overview of the number of possible configuration options regarding the database.
To configure the server please see the options below. Like in Datahike they are
read via the [environ library by weavejester](https://github.com/weavejester/environ).
Please provide the logging level without colon. Beware that a configuration file
overwrites the values from environment and properties.

envvar                        | default
------------------------------|-------------
DATAHIKE_JDBC_PORT            | PORT || 4000
DATAHIKE_JDBC_LOG_LEVEL       | warn
DATAHIKE_JDBC_DEV_MODE        | false
DATAHIKE_JDBC_TOKEN           | --
DATAHIKE_JDBC_DBTYPE          | postgresql
DATAHIKE_JDBC_HOST            | localhost
DATAHIKE_JDBC_DBNAME          | datahike
DATAHIKE_JDBC_USER            | datahike
DATAHIKE_JDBC_PASSWORD        | password
DATAHIKE_JDBC_CACHE           | 100000

### Authentication

You can authenticate to Datahike JDBC Server with a token specified via configuration. Please
then send the token within your request headers as `authentication: token <yourtoken>`.
If you don't want to use authentication during development you can set dev-mode to true
in your configuration and just omit the authentication-header. Please be aware that your
Datahike JDBC Server might be running publicly accessible and then your data might be read
by anyone and the server might be misused if no authentication is active.

### Logging

We are using the [library taoensso.timbre by Peter Taoussanis](https://github.com/ptaoussanis/timbre/) to provide
meaningful log messages. Please set the loglevel that you prefer via means
of configuration below. The possible levels are sorted in order from least
severe to most severe:
- trace
- debug
- info
- warn
- error
- fatal
- report

# Roadmap

## Release 0.2.0
- [ ] JSON support #18
- [x] Token authentication
- [ ] Implement db-tx #25
- [ ] Improve documentation #23
- [ ] Improve error messages #24
- [ ] [Clojure client](https://github.com/replikativ/datahike-client/)
- [ ] [Clojurescript client](https://github.com/replikativ/datahike-client/)

## Release 0.3.0
- [ ] Import/Export/Backup
- [ ] Metrics
- [ ] Subscribe to transactions
- [ ] Implement query engine in client

# License

Copyright © 2022 Konrad Kühne, Timo Kramer, Alexander Oloo

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.