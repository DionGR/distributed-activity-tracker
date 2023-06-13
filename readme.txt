Compile: In folder distributed-activity-tracker: 
javac .\app\backend\master\Master.java
javac .\app\backend\worker\Worker.java
javac .\dummyuser\DummyUser.java

Run: In Folder distributed-activity-tracker:
java app.backend.master.Master
java app.backend.worker.Worker
java dummyuser.DummyUser 

workerCFG, userCFG, serverCFG located in the respective /data/ folders for each component have to be manually adjusted/edited