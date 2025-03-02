# Serde Benchmarks

This project contains micro benchmarks for the serialization implementation(s).

## Testing

```sh
./gradlew :runtime:serde:serde-benchmarks:jvmBenchmark
```

Baseline `0.7.8-beta` on EC2 **[m5.4xlarge](https://aws.amazon.com/ec2/instance-types/m5/)** in **OpenJK 1.8.0_312**:

```
jvm summary:
Benchmark                                                         (sourceFilename)  Mode  Cnt   Score    Error  Units
a.s.k.b.s.json.CitmBenchmark.tokensBenchmark                                   N/A  avgt    5  12.530 ±  0.611  ms/op
a.s.k.b.s.json.TwitterBenchmark.deserializeBenchmark                           N/A  avgt    5  10.148 ±  7.515  ms/op
a.s.k.b.s.json.TwitterBenchmark.serializeBenchmark                             N/A  avgt    5   1.534 ±  1.608  ms/op
a.s.k.b.s.json.TwitterBenchmark.tokensBenchmark                                N/A  avgt    5   6.381 ±  3.615  ms/op
a.s.k.b.s.xml.BufferStreamWriterBenchmark.serializeBenchmark                   N/A  avgt    5  11.746 ±  0.262  ms/op
a.s.k.b.s.xml.XmlDeserializerBenchmark.deserializeBenchmark                    N/A  avgt    5  90.697 ±  1.178  ms/op
a.s.k.b.s.xml.XmlLexerBenchmark.deserializeBenchmark          countries-states.xml  avgt    5  22.665 ±  0.473  ms/op
a.s.k.b.s.xml.XmlLexerBenchmark.deserializeBenchmark            kotlin-article.xml  avgt    5   0.734 ±  0.017  ms/op
a.s.k.b.s.xml.XmlSerializerBenchmark.serializeBenchmark                        N/A  avgt    5  27.324 ± 31.331  ms/op
```

## JSON Data

Raw data was imported from multiple sources:

| JSON file           | Size   | Description                                                                                                                        | Source                                                                                                                  |
|---------------------|--------|------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------|
| `citm_catalog.json` | 1737KB | A big benchmark file with indentation used in several Java JSON parser benchmarks                                                  | [Java Boon - Benchmarks](https://github.com/RichardHightower/json-parsers-benchmark/blob/master/data/citm_catalog.json) |
| `twitter.json`      | 632KB  | Search "一" (character of "one" in Japanese and Chinese) in Twitter public time line for gathering some tweets with CJK characters  | [Native JSON Benchmark](https://github.com/miloyip/nativejson-benchmark/blob/master/data/twitter.json)                  |

## XML Data

Raw data was imported from multiple sources:

| XML file             | Size  | Description                                                     | Source                                                                                                                               |
|----------------------|-------|-----------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------|
| countries+states.xml | 1.3MB | A dataset of countries and states suitable for big file testing | [Countries States Cities Database](https://github.com/dr5hn/countries-states-cities-database/blob/master/xml/countries%2Bstates.xml) |
| kotlin-article.xml   | 52KB  | A Wikipedia article on Kotlin suitable for small file testing   | [Wikipedia](https://en.wikipedia.org/wiki/Special:Export/Kotlin_%28programming_language%29)                                          |

## Benchmarks

The `model` folder contains hand rolled Smithy models for some of the benchmarks. The `smithy-benchmarks-codegen` project 
contains the codegen support to generate these models.

These models are generated as part of the build. Until you run `assemble` you may see errors in your IDE.