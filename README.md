Sergeant
=======


Sergeant is a simple [DropWizard](http://dropwizard.github.io/dropwizard/) based web application to allow command line programs to be run as web services.  Yes, I know this is dangerous, but so is driving a car.  If one is careful both can be done safely.

Installation
----

#### Linux / Mac OSX:

    git clone https://github.com/dblezek/sergeant.git
    cd sergeant
    ./gradlew run

#### Windows
    git clone https://github.com/dblezek/sergeant.git
    cd sergeant
    gradlew.bat run

Open http://localhost:8080/

Usage
----
```bash
java -jar sergeant.jar server sergeant.yml
```

Defining services
----

**Services** are command line interfaces (CLI) exposed through sergeant as REST endpoints.  Services are defined using [YAML](http://www.yaml.org/spec/1.2/spec.html) (YAML Ain't Markup Language) in the **sergeant.yml** file, and have several parameters.

- **endPoint**: this is the REST end point that exposes this CLI
- **description**: a description of the endPoint.  [Descriptions can span multiple lines.](http://stackoverflow.com/questions/3790454/in-yaml-how-do-i-break-a-string-over-multiple-lines)
- **commandLine**: an array of parameters that form the command line.  Any parameter of the form "@variable" will be replaced when sergeant runs the CLI.  See the **sleep** example below.
- **synchronous**: return results immediately
- **defaults**: a map of default values for any variables in the command line.  This parameter is optional, and variables do not need defaults.

### Synchronous services

Synchronous services return the output immediately.
```bash
curl -POST -d "text=Something going on here" http://localhost:8080/rest/service/echo
Something going on here
```

### Asynchronous services
By default, services return a uuid that can be used to retrieve results after processing has finished.

### Service Environment
Sergeant includes a `sergeantw` a wrapper for sergeant services.  `sergeantw` looks for a `sergeant-env.sh` and sources it to configure a particular environment to run the CLI.  This is particularly helpful, as sargeant is often run as a daemon with a severely limited environment.

### Reloading Services

Services definitions are reloaded at a frequency specified by `reloadTimeInSeconds`.  Allows new services to be added on the fly.  Existing services are updated, and obsolete services are removed.

### Reaping Completed Status

Services that are running or completed hold their status in memory.  By default, completed jobs (success or fail) are reaped every hour.  This may be controled by the `reapTimeInSeconds` in the `sergeant.yml` file.


REST
----
Each service creates some REST APIs.  `sleep` is the service used in this example.

Method  |  URL        | Description
------- | ----------  | -------
GET     | /rest/service | JSON description of all services
GET     | /rest/job     | JSON description of all jobs running and compteled that have not been reaped.
GET     | /rest/service/sleep | Returns the definition of the service
POST    | /rest/service/sleep | Starts executing the service.  If synchronous, blocks until the service completes.  If asynchronous, returns a `uuid` that is used to lookup the status of the job.
GET     | /rest/job/`uuid` | Returns job status.
DELETE  | /rest/job/`uuid` | Deletes the job, killing if still running.

Examples
----

### Sleep Example

Definition of the sleep service in `sergeant.yml`.

```yml
workers:
  - endPoint: sleep
    commandLine: ["sleep", "@seconds"]
    description: "Powernap"
    defaults:
      seconds: 120
```

Invoking the sleep service.  NB: `curl` converts a `POST` command into a `GET`, if no parameters are passed using the `-d` flag.  The best workaround is to pass some dummy value.  Sergeant ignores any unknown substitutions.

```json
curl -POST -d seconds=120 http://localhost:8080/rest/service/sleep
  {
    "uuid":"519898b5-5afb-413d-bfc3-a2ff3a94afbd",
    "commandLine":"sleep 120",
    "exitValue":-1,
    "status":"running",
    "output":""
  }
```

The response to the post includes the job `uuid` used to look up status, the `commandLine` called, `exitValue` (-1 until the status is done), `status` of the job, and `output` of the job, when completed.

Status can be fetched using the `uuid`:
```json
curl http://localhost:8080/rest/job/519898b5-5afb-413d-bfc3-a2ff3a94afbd
{
  "uuid":"519898b5-5afb-413d-bfc3-a2ff3a94afbd",
  "commandLine":"sleep 120",
  "exitValue":0,
  "status":"done",
  "output":""
}
```

Status from all jobs can be fetched:
```json
curl http://localhost:8080/rest/job
{
  "519898b5-5afb-413d-bfc3-a2ff3a94afbd": {
    "uuid": "519898b5-5afb-413d-bfc3-a2ff3a94afbd",
    "commandLine": "sleep 120",
    "exitValue": 0,
    "status": "done",
    "output": ""
  }
}
```


#### Example sergeant.yml
DropWizard describes several other [configuration options](http://dropwizard.github.io/dropwizard/manual/core.html), including logging.

```yml
# Reap completed job status every hour
reapTimeInSeconds: 3600

# Reload this config file every minute to pick up service changes
reloadTimeInSeconds: 60

# Services to expose
workers:
  - endPoint: sleep
    commandLine: ["sleep", "@seconds"]
    description: "Power nap time!"
    defaults:
      seconds: 120

  - endPoint: echo
    commandLine: ["echo", "@text"]
    description: "Is there an echo in here?"
    synchronous: true
    defaults:
      text: This space for rent
```
