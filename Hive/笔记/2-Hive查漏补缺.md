### hive提升
- Hive建表时对多分隔符的支持
    - linux本地目录`/root/hadoop_testfiles/`下的文件`test_multi_delimiters.txt`内容如下（字段之间的分隔符为`,#S`）：
        ```
        1,#$java
        2,#$c++
        3,#$hive,
        4,#$hadoop
        5,#$hdfs
        6,#$mapreduce
        7,#$azkaban
        8,#$spark
        9,#$storm
        10,#$kafka
        ```
    - 创建以`,#$`为分隔符的hive内部表`t_mulit_delimiter_test`：
        ```
        create table t_mulit_delimiter_test (id int,name string)
        row format SERDE 'org.apache.hadoop.hive.contrib.serde2.MultiDelimitSerDe' WITH  SERDEPROPERTIES ("field.delim"=",#$");
        ```
    - 将`test_multi_delimiters.txt`文件中的数据导入`t_mulit_delimiter_test`表：
        ```
        load data local inpath '/root/hadoop_testfiles/test_multi_delimiters.txt' into table t_mulit_delimiter_test;
        ```