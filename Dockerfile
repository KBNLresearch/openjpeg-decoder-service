FROM openjdk:8

RUN apt-get update && apt-get install -y git-core cmake g++ maven && apt-get clean && rm -rf /var/lib/apt/lists/*

RUN git clone https://github.com/uclouvain/openjpeg.git

RUN cd openjpeg
WORKDIR "/openjpeg"
RUN git checkout tags/v2.3.1

RUN cmake . -DCMAKE_BUILD_TYPE=Release -DBUILD_SHARED_LIBS:bool=on -DCMAKE_CXX_COMPILER=/usr/bin/cc
RUN make
RUN make install
RUN make clean
RUN ldconfig

COPY target/openjpeg-decoder-service-1.1.jar /
COPY sample-conf.yaml /

RUN mkdir "/service"

COPY . /service
WORKDIR "/service"
RUN mvn clean package

CMD java -jar target/openjpeg-decoder-service-1.1.jar server docker-conf.yaml
