# openjpeg-decoder-service

A proof-of-concept java based jp2 decoder service.

This service uses [openjpeg 2.3](https://github.com/uclouvain/openjpeg) for decoding.

It intends to be compliant to the [IIIF image API 2.1](http://iiif.io/api/image/2.1/).


## Online demo

Online viewer using leaflet see: [here](http://kbresearch.nl/imageviewer-demos/openjpeg-decoder-demo.html?id=iceland.jp2) and [here](http://kbresearch.nl/imageviewer-demos/openjpeg-decoder-demo.html)

Service location for online demo:  [here](http://openjpeg-decoder-service.kbresearch.nl/iiif-service/iceland.jp2/full/pct:10/0/default.jpg)


## Quick start using docker

To try out the service locally you can download a sample .jp2 and run the following commands

```sh
wget https://github.com/KBNLresearch/openjpeg-decoder-service/raw/master/src/test/resources/balloon.jp2
docker run -v `pwd`:/mount -p9080:9080 -it --rm renevanderark/openjpeg-decoder-service
```

Then visit: [http://localhost:9080/iiif-service/balloon.jp2/full/pct:25/0/default.jpg](http://localhost:9080/iiif-service/balloon.jp2/full/pct:25/0/default.jpg)
to see if it works.

The docker command mounts your current working directory to the container to locate your jp2 files.


## Compiling and running the service locally without docker

This service uses a recent version of [openjpeg](https://github.com/uclouvain/openjpeg) which should be built from source
on your own distribution before compiling this service.

The Dockerfile on this repository illustrates how this can be done, but I will list the steps in this section as well:

```sh
sudo apt-get install git-core cmake g++ maven

git clone https://github.com/uclouvain/openjpeg.git
cd openjpeg
git checkout tags/v2.3.0

cmake . -DCMAKE_BUILD_TYPE=Release -DBUILD_SHARED_LIBS:bool=on -DCMAKE_CXX_COMPILER=/usr/bin/cc
make
sudo make install
sudo ldconfig

cd /path/to/openjpeg-decoder-service
mvn clean package

java -jar target/openjpeg-decoder-service-1.1-SNAPSHOT.jar server sample-conf.yaml
```

Then visit: [http://localhost:9080/iiif-service/balloon.jp2/full/pct:25/0/default.jpg](http://localhost:9080/iiif-service/balloon.jp2/full/pct:25/0/default.jpg)
to see if it works.


## Configuration options

In this directory there are some sample yaml (dropwizard) configurations. The service can be configured to decode local
files: ```sample-conf.yaml```

But also files wich are first downloaded to a cache via URL: ```sample-conf-remote.yaml```

The relevant field to adapt to your environment is: ```resolveFormat```

Other configurable options involve cache settings and multi-threading options, which should be tweaked by you on your own
environment.

When experimenting with this service in a productive environment it is essential to check out a stable (tagged) version
and assign enough memory, for instance:
```
java -Xms2g -Xmx8g -jar target/openjpeg-decoder-service-1.0.jar server sample-conf.yaml
```