This is the repository for cs1660 final project.
pitt username: stw62

---------------------
Project Description and Work Flow
---------------------
1.Concerning GUI apps
First, beginning with docker. In order to build a GUI application, need X11. So I pulled from an ubuntu image that contains X11.
I use windows, so go to https://sourceforge.net/projects/xming/ and download Xming to connect to the ubuntu container.
When creating the ubuntu container, set the DISPLAY environment variable in the ubuntu container to my windows display "--env DISPLAY=192.168.1.165:0.0".
Put DATA files along with the java files.

2.Java on the image
Install java to the image using the unix command apt-get install ...
use "javac -cp *: searchGUI" to compile the searchGUI.java along with .jar files on UNIX
then when creating the container, CMD executes "java -cp *: searchGUI" to run searchGUI including the .jar files on UNIX

3.Google cluster
Watch the video, full walkthrough included, including:
- Cluster Shown
- Cluster Storage Bucket (JAR folder containing .jar files to be executed by app)
- Credentials (Using OAuth 2.0 Client IDs, API Keys, and https://developers.google.com/oauthplayground/ to let app access GCP, not ACCESSTOKEN is an environment variable that needs to be provided to the docker container)
- Job created and executed.

3.Using apache http client and Google REST API to access Storage Bucket, Jobs.
- uses HttpGet, HttpPost, HttpDelete, HttpPatch for different request.
- Each request has own url, parameters, queries that can be looked up on https://cloud.google.com/dataproc/docs/reference/rest and https://cloud.google.com/storage/docs/json_api/v1
- not going to elaborate on this. Pain in the ass when actually using it...

4.Java Application uses Java Swing.

5.Actual algorithm implementation to meet ALLL extra credit requirements
1. SSInvertedIndex uses secondary sort:
(i) Mapper uses default "textInputFormat" reads lineoffset and line content, and outputs TERMDOCPAIR and IntWritable(which is always 1);
    TermDocPair is a class that extends "Writable, WritableComponent", and is a composite key containing natural key "term" and value key "docName".
    TermDocPair overrides compare to method that first sorts "term" using String compareTo, then sorts "docName" using String compareTo.
    NaturalKeyPartioner is a class that extends "Partioner", and partitions the <key, value> pair outputted by SSIIMapper according to natural key "term".
    This way, the pairs with the same "term" go to the same reducer.
    Reducer then takes the sorted, shuffled, merged, partioned TermDocPair, Iterable<IntWritable> and outputs [Term, Doc, Sum of Iterable];

2. SSInveretedIndex uses counters and logs:
(ii) first set up enumeration COUNTER that consists of WORD_COUNT and MAPPER_COUNT.
     WORD_COUNT records count of words by all mappers.
     MAPPER_COUNT records count of number of mappers.
     I could have one more REDUCER_COUNT but i'm too lazy to add one more.
     Each mapper gets counter from context and in the main engine, job.getCounter then print.
     After each Reducer constructs their output, uses copyMerge to merge it into 1 invertedindex.txt file.
(ii) logs are displayed after both invertedindex and TopN jobs, since logs are automatically stored in bucket's "google-cloud-dataproc-metainfo/"
     Use a GET request to pull it to app and display. EZ

3. Search Term:
	After constructing the invertedindex.txt file, use HttpGet request to pull it from the bucket.
	Then run a simple Java algorithm to find the line that contains the [Term docName frequency]

4. TopN.java:
(iii) TopN has input path of the output files of SSInvertedIndex 
      TopN is implemented using a SortedMap with TreeMap<Integer, List<String>> implementation.
      The Key of treemap is the frequency of a term, while the value is a List of terms. Why a list? Because the frequency might be the same for 2 terms.
      Treemap maintains size N by removing firstkey() since it is sorted and firstkey() has lowest frequency.
      Important: job.setNumReduceTasks(1);
      Similar to InvertedIndex, uses copyMerge(don't really need to use it because only 1 reducer!! But anyway, too lazy to fix it)


---------------------
dockerfile
---------------------
FROM ubuntu:16.04
WORKDIR /usr/src/app
COPY * ./

ENV ACCESS_KEY null

RUN apt-get update && apt-get install -y default-jdk && javac -cp *: searchGUI.java

CMD ["java", "-cp", "*:", "searchGUI"]
-------------------------------------------------------------------------------------------------------

---------------------
docker queries
---------------------
docker build --tag searchguiimage:1.0 .
docker run --privileged --env DISPLAY=192.168.1.165:0.0 --env ACCESS_TOKEN=ya29.a0Ae4lvC0mFE_CpLz0X5tg9xVDhBEBaLxeB3KNYYuyJO2R9mzstG7-T-rqAPuiKuQS774JSSv0QvdVrUydPXGvmBGMW5baJRhPxJVUSQHerNgHCQ7Th1GShtmvikS_PKzsJsF6Dn12xQs8kL-mTsyx1MvXfYQ7oZ7t0IE --name searchguicontainer searchguiimage:1.0  
-------------------------------------------------------------------------------------------------------

---------------------
OAuth 2.0 Client IDs for generating the ACCESSTOKEN
---------------------
api key
(embedded into program already, should be programmed outside of the java application, and set as an environment variable to the docker container. But too lazy to do it...):
AIzaSyCz2QoTEbTHRZK1F8Zq_w3cAzBYr_EHR1A

other client 1:
client id: 781616316318-p4pt2ahd8k3oftb4qkhml1638hbcalkl.apps.googleusercontent.com

secret: FFDM4wJ0G4S4Im31uC7Vspsy
-------------------------------------------------------------------------------------------------------

---------------------
Youtube Video Link
---------------------

-------------------------------------------------------------------------------------------------------