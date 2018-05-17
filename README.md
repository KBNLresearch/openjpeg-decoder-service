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


## Compiling and running the service locally


## Configuration options