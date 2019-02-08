# AutoComplete feature using nGram algorithm
a demonstration of Autocomplete feature using nGram algorithm 
https://bit.ly/2Decuh7

## tl&dr
- this is a full stack project(backend, frontend, database) that will demonstrate the search engine autocomplete feature
- I use AWS EMR, S3, RDS to get the output, but you can use any infrastrure. 
- VERY IMPORTANT: if you use AWS EMR like I do, Remember to TERMINATE instances after job complete. 


## about nGram
Search functionality is one of the most important feature in web applications because it enhances user experiences by helping 
them to find the right content quickly and easily. The autocomplete feature (for example, Google suggestion) enhances the search
functionality by providng suggestions for the user to choose from based on user input. This feature has been widely used in online
shopping website and search engines.

### nGram example


Phrase: Today is a good day since today is Sunday

```
1-gram: 
today  2
is     2
a      1
good   1
day    1
since  1 
sunday 1

2-gram:
today is    2
is a        1
a good      1
good day    1
since today 1 

3-gram:
today is a      1
is a good       1
good day since  1
since today is  1
today is sunday 1   
```

## how the code works
### arguments:
- arg0: large data set
- arg1: output path
- arg2: numGram; number of gram
- arg3: minCount; the minimun number of occurence for 
- arg4: numWordsFollowingInput, when type a word, how many words you want to see in result

### two mapreduce job 
#### Job 1 Build NGram Library 
- take a input (large data set), build n gram library. (in this project, nGram = 3) 


The Mapper split each sentence into 2-Gram and 3-Gram 
```
This is smart because this is a smart decision
   (1st divide)
this is, is smart, smart because, because this, this is, is a , a smart, smart decision
   (2nd divide)
this is smart, is smart because, because this is, this is a , is a smart, a smart decision

 ```

The Reducer merge each 2-Gram and 3-Gram  to count into the final count of how many times the  N-Gram has appeared. 

```
this is          2
is smart         1
smart because    1
because this     1
this is          1
is a             1
a smart          1
smart decision   1
this is smart    1
is smart because 1
because this is  1
this is a        1
is a smart       1
a smart decision 1 

```


#### Job 2: Build Language Model

The language model computes the probability of a word appearing after a phrase. For example, when user types in the phrase “this is a”, the model would predict the next word to be “smart” based on the N-Gram library we built in the last step. 


- The mapper takes input from Job1,  splits the N-Gram library into key and value, where key is the user input (starting word/phrase), and the valus is the following N-Gram with count. To get more accurate result, the N-Gram count ignores phrases that appear below minCount

- The reducer merges the mapper output into a table that has the probability of each phrase appearing after each user input. For a given phrase, we store only the top ( numWordFollowing Input, default to 5)  words with the highest probabilities . If two words have the same probability, choose the one which is lexicographically higher i.e. 'ab' comes before 'bc'.

![](https://s3-us-west-2.amazonaws.com/donot-delete-github-image/Screen+Shot+2019-02-03+at+7.53.47+AM.png)


- The reducer writes result to the mySQL database



#### Predict the next phrase using MySQL
Write the reducer output to mySQL database. 

![](https://s3-us-west-2.amazonaws.com/donot-delete-github-image/Screen+Shot+2019-02-03+at+8.09.31+AM.png)

using mySQL select statement to predict the next phrase 

![](https://s3-us-west-2.amazonaws.com/donot-delete-github-image/Screen+Shot+2019-02-03+at+8.09.37+AM.png)

#### Build a search engine with autocomplete feature
autocomplete feature for the search engine. As soon as the user enters a word into the input field, the browser will issue an AJAX request to the MySQL server by passing the current content of that field as the parameters. The MySQL server will evaluate the content and returns a list of suggestions to the browser. The browser then displays a list of words below the input field that offers the suggestions to the user. If user clicks on one of the suggestions, it will be copied into the search engine input field. 

## how to run this project
My suggested integrating steps: 
1. work with local mysql db (ex. MAMP) and local code
2. work with RDS(mysql) and EMR
3. stop EMR and RDS
4. build frontend

### local mysql DB and local code
1. clone this repo, let maven resolve depency with pom.xml, build jar
2. setup MySQL MAMP, and run the following:
```
## enter mysql 
cd /Applications/MAMP/Library/bin/ $ (mac)
sudo ./mysql -uroot -p
## run commands to setup database and table
create database test;
use test;
create table output(starting_phrase VARCHAR(250), following_word VARCHAR(250), count INT);
CREATE USER 'root'@'%' IDENTIFIED BY 'password';
GRANT ALL PRIVILEGES ON *.* TO 'root'@'%' WITH GRANT OPTION;
FLUSH PRIVILEGES;
```
3. run the jar and make sure result write to the mysql output table
```
java -jar autocomplete_ngram-1.0-SNAPSHOT.jar ../bookList ../output 2 3 4 
```
### AWS RDS and EMR
1. setup AWS RDS MySQL, free tier should be sufficient for this project
2. change the security group so inbound mysql connection is from everywhere ( Warning: this is not a production ready setup)

![](https://s3-us-west-2.amazonaws.com/donot-delete-github-image/Screen+Shot+2019-02-06+at+8.13.15+AM.png)

3. setup RDS 
- Note: RDS doesn't allow certain privileges being applied to the information tables, even by root user. Instead, choose the privileges you need. 
[Learned from here](https://www.flydata.com/blog/access-denied-issue-amazon-rds)

```
## enter mysql 
mysql -h <rds-endpoint> -P 3306 -u <rds-master-user-name> -p
<rds-master-user-password>
## run commands to setup database and table
create database test;
use test;
create table output(starting_phrase VARCHAR(250), following_word VARCHAR(250), count INT);
CREATE USER 'emr'@'%' IDENTIFIED BY 'password';
grant select,insert,update,delete,create,drop on *.* to 'emr'@'%';
FLUSH PRIVILEGES;
```

4. Setup EMR
- Create a EC2 keypair PEM file to used for EMR
- Create a S3 bucket  
- Upload the jar in this repo to your s3 bucket ( You can make change and compile your own as well)
- Upload the input files to the same s3 bucket 
- Create a EMR cluster, choose EMR version that uses hadoop version 2.7.3 (to use a different hadoop version, change the pom.xml) 
- wait for EMR provision finish:
- add a step for custom jar. for JAR location, point to the jar in the s3 bucket. for argument:
```
s3://<your-bucket>/<your-input-folder> /out <numGram> <minCount> <numWordsFollowingInput>

example:
s3://my.mapreducebucket/wikipedia_data /out 3 4 5  

```
5. After job complete, check it writes to RDS output table

![](https://s3-us-west-2.amazonaws.com/donot-delete-github-image/Screen+Shot+2019-02-07+at+9.33.10+AM.png)

6. if wish to perform frontend section, perform database dump and then terminate RDS 
```
mysqldump -h <rds-endpoint> -u <db-user-name> -p --port=<your-port> --databases <your-database >> datadump.sql
```

### Frontend 
I build my frontend with code inspired from [here](http://www.bewebdeveloper.com/tutorial-about-autocomplete-using-php-mysql-and-jquery).   
I host the mysql app [here](https://www.000webhost.com/cpanel-login) for free. 
Checkout [Demo for this project](https://bit.ly/2Decuh7). 



