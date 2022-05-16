# EGA FUSE Client
This repository contains a Java Native Runtime (JNR) based Filesystem in Userspace (FUSE) client to access the EGA Data REST API. Such FUSE client will allow access to authorized EGA-Archive files by presenting them in a virtual directory, where they can be used like regular files, without first having to download them.
## Index
* [Supported platforms](#Supported-platforms)
* [Prerequisites and dependencies](#Prerequisites-and-dependencies)
* [Building the project](#Building-the-project)
* [Running the project](#Running-the-project)
* [Making use of the mounted environment](#Making-use-of-the-mounted-environment)
* [Common issues and troubleshooting](#Common-issues-and-troubleshooting)

## Supported platforms
* Linux (Debian/Fedora)                                                         
* macOS (via [osxfuse](https://osxfuse.github.io/))
* Windows (via Windows Subsystem for Linux version 2 - [WSL2](https://codefellows.github.io/setup-guide/windows/))

## Prerequisites and dependencies
This tool was programmed in Java, and depends on the following software:

| Software        | Tested version  | Operating system  |
| -------------: |:-------------|:-------------|
| [Java](https://www.java.com/) | OpenJDK 11.0.10 | Linux and macOS |
| [Maven](https://maven.apache.org/)  | Apache Maven 3.6.3 | Linux and macOS |
| [libfuse](https://github.com/libfuse/libfuse) | libfuse-dev 2.9.9-3 | Linux |
| [osxfuse](https://osxfuse.github.io/) | 3.11.2 | macOS |
| [macfuse](https://osxfuse.github.io/) | 4.2.5 | macOS |

Column "Tested version" represents each software's version used on our latest tests, serving as a reference. Nevertheless, we encourage you to download their current version and check its functionality. Please note that newer MacOS versions (e.g. Monterey) might require macfuse instead of osxfuse.

### Installing dependencies

Herebelow you can find commands to **check** if the dependencies have already been installed, and **how to install** them if not. For each of them, the first command should prompt the version of the installed dependency. If not installed, run its second command to install it. 

#### 1. Linux
Depending on your Linux distribution (Ubuntu or RedHat) use its corresponding commands.

#### 1.1 Ubuntu
Run the following command before any installation.
``` bash
sudo apt update
```
Checking installation and installing Java, Maven and libfuse (respectively).
``` bash
# Java 
java -version
sudo apt install default-jdk

# Maven
mvn -v
sudo apt-get install maven

# Libfuse
fusermount -V
sudo apt-get install libfuse-dev
```
#### 1.2 RedHat
Run the following command before any installation.
``` bash
yum update
```
Checking installation and installing Java, Maven and libfuse (respectively).
``` bash
# Java 
java -version 
yum install java-11-openjdk

# Maven
mvn -version
sudo dnf install maven

# Libfuse
fusermount -V
yum install fuse fuse-devel
```
#### 2. macOS
Run the following command before any installation.
``` bash
softwareupdate -i -r
```
Check that the package installer Homebrew is installed:
``` bash
brew -v
```
Checking installation and installing Java, Maven and osxfuse (respectively).
``` bash
# Java 
java -version
brew install java

# Maven
mvn -v
brew install maven

# Osxfuse
system_profiler -detailLevel mini SPFrameworksDataType
brew install --cask osxfuse

# macfuse
brew install --cask macfuse
```

## Building the project
First, clone the project to your local machine in any directory of your choice. You can either download the repository manually as a ZIP file (go to `Code` at the top of this page) that you should then uncompress, or use the following `git` command.
``` 
git clone https://github.com/EGA-archive/ega-fuse-client.git
```

Then go inside the cloned directory and build the project using the commands below. It will produce the executable `.jar` file in the `/target` directory.
```
cd ega-fuse-client/
mvn clean install
```

## Running the project

The virtual directory in which files will be stored by default is `/tmp/mnt`, and thus you need to make sure it exists in your local machine before running the project. If it does not exist, create it. You can also specify a different mounting path (take a look at the [optional arguments](#Optional-arguments)).

At this point, we are ready to run the project. To do so we simply need to run the `ega-fuse-client-<version>-SNAPSHOT.jar` file we just created within the `target/` directory. This executable file accepts several [optional arguments](#Optional-arguments), which can be displayed running: 
``` bash
java -jar target/ega-fuse-client-2.1.1-SNAPSHOT.jar --h
```

As we mentioned before, the only datasets that will appear in the mounted directory `/tmp/mnt` are the ones we have access to (i.e. we have been authorised), so we need to provide our credentials to the fuse-client. These credentials are the same ones you use to download datafiles through the EGA download client [PyEGA3](https://github.com/EGA-archive/ega-download-client). We can either:
1. Run the command without giving a credentials file (we will be asked to write our credentials in the terminal)
````
java -jar target/ega-fuse-client-2.1.1-SNAPSHOT.jar
````
2. Run the command handing over such file with our credentials (its format can be found in the [optional arguments](#Optional-arguments) section). For the sake of testing, there is an existing ``test_credentials.txt`` file you can use, which should be replaced with your own credentials file. 
```
java -jar target/ega-fuse-client-2.1.1-SNAPSHOT.jar --cf='test_credentials.txt'
```

Additionally, instead of running the `.jar` file yourself, you can make use of the ``fuseclient.sh`` script. This script will manage the execution of the ``.jar`` file and can be used to start, stop or restart it. All additional arguments can be given as a block between brackets (e.g. `"--cf=test_credentials --m=/tmp/mnt ..."`). 
``` bash 
# To start the project
./fuseclient.sh start "--cf=CREDENTIAL_FILE_PATH"

# To find the process we started and kill it
./fuseclient.sh stop

# Combining the two above
./fuseclient.sh restart "--cf=CREDENTIAL_FILE_PATH"
```

The standard output of this execution can be found at `fuse-client-logs.log`.

No matter what method you use to start the project, once it is running a virtual directory will be created at the given mounting path (default path:``/tmp/mnt``). This virtual directory will contain your authorised EGA datasets and files. You can now open another terminal (if the execution was not run in the background) and proceed to the [making use of the mounted environment](#Making-use-of-the-mounted-environment) section. 

### Optional arguments:
* --m : mount point path. Default value: ``/tmp/mnt``. **Note**: Ensure that the mount point path exists`
* --cache : the maximum size of the cache. Default value: ``100``. This value means `100 * 10 MB = 1000 MB`.
* --c : maximum number of API connections used by the application. Default value: ``4``.
* --cpf : connections per file (download a file using the specified number of connections). Default value: ``2``. **Note**: preferably, ``c`` (connections) >= ``cpf`` (connections per file).
* --t : toggle tree structure. Shows files with original submitter directory structure (enable) or all the dataset files in a single directory (disable), default value: `enable`
* --h : help. Show this help message and exit.
* --cf : credentials file containing username and password. Its format can be seen below or within `test_credentials.txt`. **Note**: If no credential file was provided it will prompt the user for username and password.
```
username:ega-test-data@ebi.ac.uk
password:egarocks
```

Example of usage (replace these ``<arguments>`` with your own values):
```
java -jar target/ega-fuse-client-2.1.1-SNAPSHOT.jar --cf=<CREDENTIAL_FILE_PATH> --m=<MOUNTPOINT_PATH> --cache=<CACHE_SIZE> --c=<CONNECTION> --cpf=<CONNECTION_PER_FILE>
```

## Making use of the mounted environment

First, we need to keep in mind that the fuse-client is just that, a client. Therefore, **anything we do on the mounted directory (remove, modify or add new files) will not affect the real and archived files**. This way, you can use the mounted directory as you wish and, if things get messy, you simply need to stop the fuse-client and start it again. 

It is **important to notice** that the mounted environment will create the folders structure on *the fly* as we list them. In other words, before changing to any directory, we will need to list the content of its father directory. 

The first step to make use of your mounted environment is listing all datasets that you have access to. In this part of the README we will be using the ``test_credentials.txt``, which grants us access to 2 datasets: **EGAD00001003338** and **EGAD00001006673**.
````bash
cd /tmp/mnt/
ls
````

Now we can enter one of these datasets and list their subdirectories. In our case we will use files associated to analysis EGAF00005007329 from dataset EGAD00001003338. 

````bash
cd EGAD00001003338/
ls
cd EGAZ00001698357/
ls 
````
Within this folder we have full access to the file `HG01775.chrY.bcf` and its index ``HG01775.chrY.bcf.csi``. At this point we can do anything with them. For instance: 
* Copying/moving this file to our local machine.
````bash
cp HG01775.chrY.bcf.csi LOCAL_PATH/
````
* Using locally installed tools (e.g. bcftools, samtools, etc.) to visualise/edit these files.
````bash
bcftools view HG01775.chrY.bcf | head
````

Please note that, since the client downloads the files you make use of on *the fly*, commands involving large files **may take some time** to be executed. 

## Common issues and troubleshooting

Remember to check check the terminal's messages if you run the project yourself, or the log file `fuse-client-logs.log` if you used the ``fuseclient.sh`` script instead.

### Mount directory left mounted: "bad mount point"
Errors like `fuse: bad mount point /tmp/mnt: Transport endpoint is not connected.` are caused by a mounting directory being left mounted after a crash of your filesystem. To solve it, dismount such directory with your corresponding command below and then start the project again.

``` bash
# Command for linux
umount -l /tmp/mnt

# Command for macOS
umount /tmp/mnt
```

**Note**: If you have used custom mount point path replace ``/tmp/mnt`` with your mount point path directory.

### Server connectivity issues: error 500

Just like [PyEGA3](https://github.com/EGA-archive/ega-download-client), the fuse-client is dependent on the ega-data-api backend service to work. Sometimes, such service is unstable or simply not available and server connections issues will arise. The associated error message may look something like the following, and you will be presented with ``Error 500``. Sadly there is no easy solution for this issues from the user's perspective, and thus you are advised to wait a few minutes and try again. 

```` 
2021-03-23 12:58:44.172 ERROR 252 --- [       Thread-6] u.a.e.e.e.service.EgaDatasetService      : status: 500
2021-03-23 12:58:44.181 ERROR 252 --- [       Thread-6] u.a.e.e.e.service.EgaDatasetService      : Error in get dataset - {"timestamp":"2021-03-23T11:58:46.082+00:00","status":500,"error":"Internal Server Error","message":""}

uk.ac.ebi.ega.egafuse.exception.ClientProtocolException: {"timestamp":"2021-03-23T11:58:46.082+00:00","status":500,"error":"Internal Server Error","message":""}
        at uk.ac.ebi.ega.egafuse.service.EgaDatasetService.buildResponseGetDataset(EgaDatasetService.java:88) ~[classes!/:2.0.1-SNAPSHOT]
        at uk.ac.ebi.ega.egafuse.service.EgaDatasetService.getDatasets(EgaDatasetService.java:61) ~[classes!/:2.0.1-SNAPSHOT]
        at uk.ac.ebi.ega.egafuse.service.EgaDirectory.read(EgaDirectory.java:86) ~[classes!/:2.0.1-SNAPSHOT]
        at uk.ac.ebi.ega.egafuse.service.EgaFuse.readdir(EgaFuse.java:60) ~[classes!/:2.0.1-SNAPSHOT]
        at ru.serce.jnrfuse.AbstractFuseFS.lambda$init$10(AbstractFuseFS.java:169) ~[jnr-fuse-0.5.4.jar!/:na]
        at jnr.ffi.provider.jffi.NativeClosureProxy$$impl$$4.invoke(Unknown Source) ~[na:na]
````


