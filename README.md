# Anonymize WTA 2021 Matches Data with ARX

## Introduction
ARX is an open source library, which provides convenient tools for anonymizing sensitive data. It could be specially useful in software development in specific areas, such as differential privacy.
This project imports the current Women's Tennis Association (WTA) 2021 matches data, selects specific attributes, and anonymizes them using the k-anonymity model from the ARX API. The WTA matches data is publicly provided [here](https://github.com/JeffSackmann/tennis_wta).

The reason why I choose to process the WTA matches data is that, it was also the source of data for my "Data Management in the Cloud" class project in my bachelors.
Back then, I developed programs to analyze the data using the SQLite and the Apache Hadoop. I think it would be interesting to analyze the same set of "big data" with ARX, so that I could compare the performance of different tools.

## ARX Library setup
I used maven to set up the ARX library and this project. Unfortunately, ARX is not available as a package in the maven central repository. So the first step to set up the ARX library is to download it from [here](https://github.com/arx-deidentifier/arx) to the local environment.
```shell
git clone https://github.com/arx-deidentifier/arx.git
cd arx
# you will then need to update the dependency in the pom.xml file
# find the dependency with the artifactId pdfbox2-layout 
# and change the version from 1.0.0 to 1.0.1
mvn install -DskipTests -Dcore=true
```

## Build and run the application
Clone the project from here, open it in an IDE, and run the Application class in the src source folder.
```shell
git clone https://github.com/kurosawa467/ARX-Application.git
```
I have tried to build an executable jar which contains all the necessary libraries, so that users could directly execute it from the command line. Specifically, I have tried the maven-assembly-plugin tool, however, it was not working (which might be because of some external libraries).

## Implementation
First of all, I read [this](https://dl.acm.org/doi/10.1142/S0218488502001648) and [this](https://ieeexplore.ieee.org/document/4221659) paper to get a general idea as what "differential privacy" and "k-anonymity" are (I have never worked on this before :) ), and the mathematics behind them.

I also cloned the original [ARX library](https://github.com/arx-deidentifier/arx.git) and read the specifications, and relevant interfaces and classes. Furthermore, I read all of the example classes which demonstrate how to run the ARX API for different anonymizing and analyzing purposes. (I actually spotted a bug in the [example16.java](https://github.com/arx-deidentifier/arx/blob/master/src/example/org/deidentifier/arx/examples/Example16.java) class. The "gender" indices should be 1 instead of 0 on line 129 and 139).

Then I started the implementation for this project. First, I chose the dataset that I would like to work on, the WTA 2021 matches data, and extracted several columns with data in different kinds of distribution for further processing. Based on their own distribution patterns, I chose different hierarchy definitions accordingly. 
To be more specific, the ```tourney_name``` and the ```winner_ioc``` had static values from sets of size around 70, so I created csv files to hardcode define the hierarchies. 
One example for each hierarchy is given as below:
```
tourney_name: Abu Dhabi,West Asia,Asia,*
winner_ioc: GER,Central Europe,Europe,*
```
The ```winner_id``` and ```winner_name``` values contained common prefixes as digits or characters, so I chose to use redaction based hierarchy builder to anonymize data by simply replacing characters with '*' from right to left.
```
winner_id: 206368,20636*,2063**,206***,20****,2*****,******
winner_name: Veronika Kudermetova, Veronika Kudermetov*, ...... ,**************************
```
The ```match_num``` ranged mostly between 200-300, with exceptions from 0-200. For this kind of categorical attribute, I used order based hierarchy builder to map values to ranges. And the ```winner_age``` attribute distributed continuously from 15 to 41, therefore, I used interval based hierarchy builder to define the hierarchy.
```
match_num: 184,[179, 188],[169, 299],[100, 300],*
winner_age: 29.8015058179,[25.0, 30.0[,[15.0, 40.0[,*
```
With the defined hierarchies, it was not so difficult to use the ARX API to anonymize the data using the k-anonymity model. I configured the model with k being 4 and the suppression limit being 0.02 (I have to admit, this is rather random). And the result, together with the processing time the program took, would be printed out in the console.

## Observations
Here is the first ten lines of the output.
```
Total processing time: 0.281s
Anonymized WTA 2021 matches data:
   [tourney_name, match_num, winner_id, winner_name, winner_ioc, winner_age]
   [*, [100, 300], 21****,        **************************, East Europe, [20.0, 25.0[]
   [*, [100, 300], 20****,        **************************, East Europe, [20.0, 25.0[]
   [*, [100, 300], 20****,        **************************, Southeast Europe, [25.0, 30.0[]
   [*, [100, 300], 21****,        **************************, East Europe, [20.0, 25.0[]
   [*, [100, 300], 21****,        **************************, East Europe, [15.0, 20.0[]
   [*, [100, 300], 20****,        **************************, East Europe, [20.0, 25.0[]
   [*, [100, 300], 21****,        **************************, North America, [20.0, 25.0[]
```
As we can see from the output above, the data is anonymized as I expected. The right four digits of the winner_id attributes are replaced with the asterisk. The match_num and the winner_age are mapped to ranges and intervals accordingly. The tourney_name and the winner_ioc attributes are anonymized as defined in its hierarchy definition.
The winner_name is anonymized to the highest anonymous hierarchy value, with all characters redacted and replaced with '*'.

There is actually not much worth comparing the ARX with SQLite and the Apache Hadoop, as I later realized that they serve different purposes. SQLite and Hadoop are largely used to aggregate the original data, and ARX anonymizes the data record row by row.
Nevertheless, I was impressed by the ARX processing time. It only took ARX 0.2 - 0.3 seconds to build the hierarchies, go through the 2500-line csv file, and anonymize the attributes. According to my old project report, however, when using SQLite to aggregate files about the same size, it took around 1.6 seconds. When using aggregate and map-reduce functions in Hadoop, it could take even more than 1 minute.
I believe this shows that ARX has potentials to anonymize big size of data, with possibilities to scale up and be employed in the cloud to adapt to even bigger set of data.

## Challenges
One of the challenges that I came across was that, the source code was not well documented. For example, when defining a hierarchy using the hierarchy builder, before I could call ```build()``` method on the builder instance to get a hierarchy instance, I would need to call ```prepare(data)``` to initialize the builder instance. The ```data``` parameter should be an array of strings of all the actual attribute values. This was not documented in the javadoc, but I found out later after getting error messages and read through the method implementations in the library.
Furthermore, the ```data``` array should not contain duplicate values. I found it out from an inline comment inside the method implementation (```HierarchyBuilderGroupingBased.java```, line 410. [link](https://github.com/arx-deidentifier/arx/blob/master/src/main/org/deidentifier/arx/aggregates/HierarchyBuilderGroupingBased.java)). However, I believe that this should be specified in the javadoc.

Another challenge for me was that, when configuring the hierarchy, I was not aware of the statistics principles but rather used my incentives (as my background is in computer science, not data science :) ). For example, the winner_age ranges from 15 to 41, and I defined the hierarchy with an interval based hierarchy builder using intervals of 5. So the result of anonymization in intervals like [15.0, 20.0], or [20.0, 25.0]. Does it really make sense, especially in statistics? I am not so sure.

In the end, this project was a nice learning experience. I appreciate the opportunity to learn more about the ARX library, to make use of the ARX API to anonymize real data, and to compare it with some tools that I have used before. When I had the project running and producing a meaningful result, it was not only fun, but more of a sense of achievement.

## Credits
[1] ARX library. [link](https://github.com/arx-deidentifier/arx).

[2] WTA tennis rankings. [link](https://github.com/JeffSackmann/tennis_wta).
