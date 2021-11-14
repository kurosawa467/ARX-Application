# Anonymize WTA 2021 matches data with ARX

## Introduction
ARX is an open source library, which provides convenient tools for anonymizing sensitive data. It could be specially useful in software development in specific areas, such as differential privacy.
This project imports the current WTA 2021 matches data, selects specific attributes, and anonymizes them using the k-anonymity model from the ARX API. The WTA matches data is publicly provided [here](https://github.com/JeffSackmann/tennis_wta).
<br />
<br />
The reason why I choose to process the WTA matches data is that, it was also the source of data for my "Data Management in the Cloud" class project in my bachelors.
Back then, I developed programs to analyze data using the SQLite and the Apache Hadoop. I think it would be interesting to analyze the same "big data" using a new tool (ARX) and compare them.

## ARX Library setup
I used maven to set up the ARX library and this project. Unfortunately, ARX is not available as a package in the maven central repository. So the first step to set up the ARX library is to download it in the local environment.
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
I have tried to build an executable jar, so that users could directly execute it from the command line. However, it is not working (which might be because of some external libraries).


## Implementation
First of all, I read [this](https://dl.acm.org/doi/10.1142/S0218488502001648) and [this](https://ieeexplore.ieee.org/document/4221659) paper to get a general idea as what "differential privacy" and "k-anonymity" are (I have never worked on this before :) ), and the mathematics behind them.
<br />
<br />
I also cloned the original [ARX library](https://github.com/arx-deidentifier/arx.git) and read the specifications, and relevant interfaces and classes. Furthermore, I read all of the example classes which demonstrate how to run the ARX API for different anonymizing and analyzing purposes. (I actually spotted a bug in the example16.class. The "gender" indices should be 1 instead of 0 on line 129 and 139).
<br />
<br />
Then I started the implementation for this project. First, I chose the dataset that I would like to work on (which is the WTA 2021 matches data), and extracted some of the columns for further processing. Based on the distribution features of the data, I chose different hierarchy definitions accordingly. 
For example, the tourney_name and the winner_ioc have relatively static values from sets of size around 70, and I created csv files to hardcode define the hierarchies. The winner_id and winner_name vary a lot, so I chose to use redaction based hierarchy builder to anonymize data by simply replacing characters with '*' from right to left.
The match_num ranges mostly between 200-300, with exceptions from 0-200. For this categorical attribute, I used order based hierarchy builder to map values to ranges. And winner_age attribute ranges continuously from 15 to 41, therefore, I used interval based hierarchy builder to define the hierarchy.
<br />
<br />
With defined hierarchies, it is not so difficult to use the ARX API to anonymize the data using the k-anonymity model. I configured the model with k being 4 and the suppression limit being 0.02 (I have to admit, this is rather random). And the result, together with the processing time the program took, will be printed out in the console.
<br />
<br />

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
As we can see from the output above, the data is well anonymized. The right four digits of the winner_id attributes are replaced with the '*'. The match_num and the winner_age are mapped to ranges. The winner_ioc attributes are anonylized as its hierarchy definition.
The tourney_name and the winner_name are anonymized to the highest anonymous hierarchy value.
<br />
<br />
Comparing the performance of ARX with the SQLite and the Hadoop, the tools I used to process the same "big data", the most remarkable difference is the processing time. It took ARX 0.2 - 0.3 seconds to go through the 2500-line csv file and process the attributes. According to my old project report, however, when using SQLite for the same purpose, it took around 1.6 seconds. When aggregate and map-reduce functions were used in Hadoop, it could take even more than 1 minute.
I have noticed that ARX defined its own classes for processing csv files in java, which might take the credit for it.

## Challenges
One of the challenges that I came across was that, the source code was not well documented. For example, when defining a hierarchy using the hierarchy builder, before I could call build() on the builder instance to get the hierarchy, I would need to call prepare(data) to initialize the HierarchyBuilder class instance. The "data" would be an array of strings of all of the attribute values. This was not documented in the javadoc, but I found out later after getting error messages and read through the method implementations in the library.
Furthermore, the "data" array should not contain duplicate values. I found it out from an inline comment inside a method implementation (class HierarchyBuilderGroupingBased.java, line 410). However, I believe that this should be specified in the javadoc.
<br />
<br />
Another challenge for me was that, when configuring the hierarchy, I was not aware of the statistics principles but rather "incentive" (as my background is in computer science, not data science :) ). For example, the winner_age ranges from 15 to 41, and I defined the hierarchy with an interval based hierarchy builder using intervals of 5. So the result of anonymization in intervals like [15.0, 20.0], or [20.0, 25.0]. Does it make sense, especially in statistics? I don't really know.
<br />
<br />
In the end, this project was a nice learning experience. I appreciate the opportunity to learn more about the ARX library, to make use of the ARX API to anonymize real data, and to compare it with some of the tools that I have used before. When I had the project running and producing the meaningful result, it was not only fun, but more of a sense of achievement.
# Credits
