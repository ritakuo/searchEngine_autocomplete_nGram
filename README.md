# AutoComplete feature using nGram algorithm
a demonstration of Autocomplete feature using nGram algorithm 

## tl&dr
this is an full stack project(backend, frontend, database) that will demonstrate the search engine autocomplete feature


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


- The mapper splits the N-Gram library into key and value, where key is the user input (starting word/phrase), and the valus is the following N-Gram with count. To get more accurate result, the N-Gram count ignores phrases that appear below a cetain threshold. 

- The reducer merges the mapper output into a table that has the probability of each phrase appearing after each user input. For a given phrase, we store only the top 5 words with the highest probabilities . If two words have the same probability, choose the one which is lexicographically higher i.e. 'ab' comes before 'bc'.


![](https://s3-us-west-2.amazonaws.com/donot-delete-github-image/Screen+Shot+2019-02-03+at+7.53.47+AM.png)


#### Predict the next phrase using MySQL
Write the reducer output to mySQL database. 

![](https://s3-us-west-2.amazonaws.com/donot-delete-github-image/Screen+Shot+2019-02-03+at+8.09.31+AM.png)

using mySQL select statement to predict the next phrase 

![](https://s3-us-west-2.amazonaws.com/donot-delete-github-image/Screen+Shot+2019-02-03+at+8.09.37+AM.png)

#### Build a search engine with autocomplete feature
autocomplete feature for the search engine. As soon as the user enters a word into the input field, the browser will issue an AJAX request to the MySQL server by passing the current content of that field as the parameters. The MySQL server will evaluate the content and returns a list of suggestions to the browser. The browser then displays a list of words below the input field that offers the suggestions to the user. If user clicks on one of the suggestions, it will be copied into the search engine input field. 

