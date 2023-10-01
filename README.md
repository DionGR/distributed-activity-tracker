<div align="center">
    <img src="/media/image.png" width="200" >
</div>

### Description 📌
<p align="justify">Tempo is an activity tracking mobile app developed in Java using the MapReduce framework. It consists of a mobile frontend application that manages activity tracking as well as a backend system responsible for the analysis of the collected data. Moreover it enables a form of social networking among users through performance leaderboards and charts.
</p>

<p align="justify">Each user has a personal profile, where they can upload their activities. Furthermore, they can view their total statistics such as total distance, total activity time etc. </p>
<div align="center">
    <img src="/media/login.png" alt="Login screen" height="300">
    <img src="/media/results.png" alt="GPX results screen" height="300">
</div>

<div align="center">
    <img src="/media/statistics.png" alt="Statistics screen" height="300">
    <img src="/media/segments.png" alt="Available segments"height="300">
    <img src="/media/leaderboard.png" alt="Leaderboard"height="300">
</div>



### Interact with Tempo 💻📱

<details>
<summary> <b>Configurations</b> </summary>

- <p><i>workerCFG</i>, <i>serverCFG</i> located in the respective <i>/data/</i> folders for each component have to be manually adjusted/edited</p>
- <p><i>userCFG</i> located in <i>android/app/src/main/java/app/backend/user</i> has to be manually adjusted/edited</p>
- <p><i>host</i> and ports fields located in <i>/android/app/src/main/java/app/backend/AppBackend.java</i> have to be manually adjusted/edited</p>
</details>

<details>
<summary> <b>Compile</b> </summary>
In folders master and worker respectively:
  
```javac .\app\backend\master\Master.java```<br>
```javac .\app\backend\worker\Worker.java```
</details>

<details>
<summary> <b>Run</b> </summary>
In folders master and worker respectively:
  
```java app.backend.master.Master```<br>
```java app.backend.worker.Worker```

Then, run the application using a virtual or physical device (Android 8.0 - Oreo OS or more recent).
</details>


### Implementation Details 📜

#### ● GPX files
<p align="justify">
An activity is a sequence of GPS waypoints where each waypoint consists of:</p>

- its coordinates (latitude, longitude)
- its elevation
- the exact time it was recorded

This sequence of waypoints is saved in a specific XML file called GPX like thισ:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<gpx version="1.1" creator="user1">
    <wpt lat="52.2423614748556" lon="5.281985213730702">
        <ele>-0.45</ele>
        <time>2023-03-15T10:41:51Z</time>
    </wpt>
    <wpt lat="52.24078476451067" lon="5.294344832871327">
        <ele>-0.06</ele>
        <time>2023-03-15T10:43:59Z</time>
    </wpt>
    ...
</gpx>
```

<p align="justify">A GPX file contains an activity/route and its processing is done in parallel by two or more machines using the Map Reduce framework to accelerate the process.</p>

<br>

#### ● MapReduce framework
The MapReduce framework is a programming model that enables the parallel processing of large volumes of data. It is based on two functions:
- $map(key,value) \rightarrow [(key_2, value_2)]$
- $reduce(key_2,[value_2]) \rightarrow [value_{final}]$


1. Map function: 
    - The input may be all lines of a file (or part of a bigger file) as value, along with its corresponding ID as key $(key, value)$
    - The generated output is another key-value pair $(key_2, value_2)$
    - The map function is such that it can run on multiple inputs on different nodes/machines in parallel. The degree of parallelism can be adjusted


2. Reduce function:
    - Merges all intermediate results associated with the same key and produces the final result(s)
    - Its execution takes place after all map functions are completed


💡**The high-level idea of the MapReduce framework integration in this project is the following:**
- $map(ChunkID,Chunk) \rightarrow [(Distance, dist_{chunk_i}), (Time, time_{chunk_i}), (Elevation, ele_{chunk_i}), (Velocity, vel_{chunk_i})]$
- $reduce(Distance, [dist_{chunk_1}...dist_{chunk_n}]) \rightarrow dist_{total}$ etc.

    
(Chunk=part of a larger GPX file)

<br>

#### ● Backend

⬆️It is based on the MapReduce framework described above. <br>
✔️In this implementation **Master** node is also **Reducer** while **Worker** nodes are **Mappers**. 

- Master runs TCP Server to listen for Workers trying to connect or send intermediate results and it is multithreaded so as to serve many users simultaneously and communicate with Workers in the same time.
- Workers are multithreaded as well, so as to serve many requests comming from Master in parallel. 
- Master communicates with workers to make requests and receive intermediate results via TCP sockets.

<details>
<summary> <b>Descriptive slides 🖌️</b> </summary>
<img src="/media/1st_Slide.png" alt="1st Slide">

---

<img src="/media/2nd_Slide.png" alt="2nd Slide">

---

<img src="/media/3rd_Slide.png" alt="3rd Slide">

---

<img src="/media/4th_Slide.png" alt="4th Slide">

---

<img src="/media/5th_Slide.png" alt="5th Slide">

---
</details>

<br>

#### ● Frontend

⬆️Connection with backend:

- <p align="justify">Master runs TCP Server to listen for requests coming from the Application and it is multithreaded so as to serve many users simultaneously and communicate with Workers in the same time.</p>
- <p align="justify">Master communicates with the Application to receive requests and send results via TCP sockets.</p>



📱The Application: an Android application that enables users to:

- <p align="justify">Select a GPX file, stored in their device and send it to the backend to be processed asynchronously</p>
- <p align="justify">Receive notification when the processing of the file has finished</p>
- <p align="justify">View GPX results (total distance, mean velocity, total elevation, total time)</p>
- <p align="justify">View their personal statistics compared to other users' statistics (side by side barchart for each performance metric) as well as the relevant percentage difference (<I>e.g.</i> +31% compared to others)</p>
- <p align="justify">Select a route segment (basically a GPX file deemed to be part of a route and thus referred to "segment") and upload it to the backend so that every time a user makes this route (as part of a longer route) their performance is recorded and their best performance (if many) appears on a leaderboard. <i>e.g.</i> If you have uploaded a segment A-B-C-D (declaring you are intersted in keeping track of your performance on that segment compared to others), then you will be able to view a leaderboard for that segment, featuring all users that have walked by A-B-C-D as well (but only their best attempt, if many)
    
    <u><i>Note</i></u>: GPS drift has been taken into account </p>
