# DIY CQRS Javalin example

A sample project implementation using [DIY CQRS](https://github.com/SuppieRK/DIY-CQRS) library.

Similar functionality implemented with Spring [available here](https://github.com/SuppieRK/DIY-CQRS-Spring-Counterpart).

## How to launch?

```shell
./gradlew clean spotlessApply build jibDockerBuild && docker compose up
```

## How to play with?

- http://localhost:8080/swagger - service OpenAPI specification.
- http://localhost:9090 - Prometheus UI.
- http://localhost:3000 - Grafana UI
    - Login: `admin`
    - Password: `grafana`

## How to turn it off?

```shell
docker compose down -v
```

## How to play with the load?

- Install [Grafana k6](https://grafana.com/docs/k6/latest/set-up/install-k6/).
- Use one of the [IDE extensions](https://grafana.com/docs/k6/latest/misc/integrations/#ide-extensions)
  or [CLI](https://grafana.com/docs/k6/latest/get-started/running-k6/#run-local-tests) to kick off `load-test.js`
  script.
