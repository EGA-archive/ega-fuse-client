# EGA FUSE Client
This is a Java Native Runtime (JNR) based Filesystem in Userspace (FUSE) client to access the EGA Data REST API. This client will allow access 
to authorized EGA Archive files by presenting them in a vitual directory, where they can be used like regular files, 
without first having to download them.

## Prerequisite dependencies
1. Maven
2. Java

#### Linux

[`libfuse`](https://github.com/libfuse/libfuse) needs to be installed.

#### Ubuntu
```
sudo apt-get install libfuse-dev
``` 

#### MacOS

[`osxfuse`](https://osxfuse.github.io) needs to be installed.

```
brew cask install osxfuse
```


## Build the project

To build the project run the command below. It will produce the executable jar file in the /target directory.
```
mvn clean install
```

## Run the project

Use the command below to run the jar
```
java -jar target/ega-fuse-client-2.0.0.jar --cf=CREDENTIAL_FILE_PATH
```
or
```
java -jar target/ega-fuse-client-2.0.0.jar --cf=CREDENTIAL_FILE_PATH --m=MOUNTPOINT_PATH --cache=CACHE_SIZE --c=CONNECTION --cpf=CONNECTION_PER_FILE
```
or

In Linux the fuse layer can also be started, stopped and restarted using shell script ./fuseclient.sh as:

```
./fuseclient.sh start "--cf=CREDENTIAL_FILE_PATH"
```
 
```
./fuseclient.sh restart "--cf=CREDENTIAL_FILE_PATH"
```

``` 
./fuseclient.sh stop
```

Optional arguments:
* m : mount point path, default value: /tmp/mnt `Note: Ensure that the mount point path exists`
* cache : the maximum size of the cache, default value: 100 `Means 100 * 10 MB = 1000 MB`
* c : connections, maximum number of API connections used by the application, default value: 4
* cpf : connections per file, download a file using the specified number of connections, default value: 2, `preferably, c (connections) >= cpf (connections per file)`
* t : toggle tree structure, Shows files with original submitter directory structure (enable) or all the dataset files in a single directory (disable), default value: `enable`
* h : help, show this help message and exit
* cf : credential file below format
```
username:ega-test-data@ebi.ac.uk
password:egarocks
```

`Note` : If no credential file was provided it will prompt the user for username and password.

### Troubleshoot fuseclient.sh
Check the log file fuse-client-logs.log. If you see any error as `fuse: bad mount point /tmp/mnt: Transport endpoint is not connected.`, try running the command below

```
umount -l /tmp/mnt
```

## Supported platforms
* Linux                                                         
* MacOS (via [osxfuse](https://osxfuse.github.io/))
