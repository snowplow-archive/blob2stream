# blob2stream

blob2stream is a command line application that can copy text or binary data from blob storage to stream.

Following options are supported:
- from S3 to Kinesis on AWS
- from GCS to PubSub on GCP

## Quickstart

 ```bash
 $ git clone git@github.com:snowplow-incubator/blob2stream.git
 $ cd blob2stream
 $ sbt clean assembly
 $ java -jar target/scala-2.13/blob2stream-{{version}}.jar
 ```


## Copyright & License
blob2stream is copyright 2021 Snowplow Analytics Ltd.

Licensed under the **[Apache License, Version 2.0][license]** (the "License");
you may not use this software except in compliance with the License.

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

[license]: http://www.apache.org/licenses/LICENSE-2.0
