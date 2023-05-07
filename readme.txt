Compile: In folder distributed-activity-tracker: 
javac .\master\Master.java
javac .\worker\Worker.java
javac .\user\DummyUser.java

Run: In Folder distributed-activity-tracker:
java master.Master
java worker.Worker
java user.DummyUser 

workerCFG, userCFG, serverCFG located in the respective /data/ folders for each component have to be manually adjusted/edited