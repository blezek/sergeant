Sergeant
=======


Sergeant is a simple DropWizard based Web application to allow command line programs to be run as web services.  Yes, I know this is dangerous, but so is driving a car.  If one is careful both can be done safely.

Usage
-----
    java -jar sergeant.jar server sergeant.yml

Defining services
-----------------
Add a section to sergeant.yml

    workers:
      - endPoint: registration
        commandLine: ["python", "-iterations", "@iterations", "@PatientCode"]
        defaults:
          foo: "this is foo"
          bar: "this is bar"
          iterations: 10000

      - endPoint: sleep
        commandLine: ["sleep", "@seconds"]
        defaults:
          seconds: 120

      - endPoint: echo
        commandLine: ["echo", "@text"]
        synchronous: true
        defaults:
          text: This space for rent



Reloading Services
------------------
    reloadTimeInSeconds: 30

Reload all services every 30 seconds.  Allows new services to be added on the fly.  Existing services are updated, but obsolete services are not removed.

List of services can be found at http://localhost:8080/rest/service

Services
--------
Each service has several options

- **endPoint**: name of the service, http://localhost:8080/rest/service/endPoint
- **commandLine**: command line to run
  - any parameters of the form @key will be replaced by POST parameters
- **defaults**: map of command line defaults
- **synchronous**: return results immediately

### Synchronous services

Synchronous services return the output immediately.

    curl -POST -d "text=Something going on here" http://localhost:8080/rest/service/echo
    Something going on here




## Asynchronous services
By default, services return a uuid that can be used to retrieve results after processing has finished.

### Sleep
    curl -POST -d seconds=120 http://localhost:8080/rest/service/sleep
    {
       "uuid":"519898b5-5afb-413d-bfc3-a2ff3a94afbd",
       "commandLine":"sleep 120",
        "exitValue":-1,
       "status":"running",
       "output":""
    }

The response to the post includes the job `uuid` used to look up status, the `commandLine` called, `exitValue` (-1 until the status is done), `status` of the job, and `output` of the job, when completed.

Status can be fetched using the `uuid`:

    curl http://localhost:8080/rest/job/519898b5-5afb-413d-bfc3-a2ff3a94afbd
    {
       "uuid":"519898b5-5afb-413d-bfc3-a2ff3a94afbd",
       "commandLine":"sleep 120",
        "exitValue":0,
       "status":"done",
       "output":""
    }

Status from all jobs can be fetched:

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
