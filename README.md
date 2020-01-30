# EGA FUSE Client

This is a Java-(JNR)-based FUSE client for the EGA Data REST API (v3). This client will allow access to authorized EGA Archive files by presenting them in a vitual directory, where then can be used like regular files, without first having to download them.

## Getting Started

These instructions will get you a copy of the project up and running on your local machine.

### Installing

The repository contains pre-compiled jar files with the client. To build it on your local machine, run

```
mvn package
```

This will produce the FUSE later jar file in the /target directory.

### Mac Specific Installation:

This FUSE layer has been tested on Mac OS as well. You must install Fuse for Mac / Osxfuse: https://osxfuse.github.io/ 

### Windows Specific Installation:

This FUSE layer has been tested on Windows OS as well. You must install WinFSP: https://github.com/billziss-gh/winfsp/releases/download/v1.2POST1/winfsp-1.2.17346.msi

## Starting the FUSE layer

The FUSE layer requires at minimum a valid EGA OAuth2 Bearer Token to run; specifying a mount directory is recommened (default is /tmp/mnt). The mount directoty must be an existing and empty directory:

For Linux & Mac:

```
java -jar ega-fuse-0.1.0.jar -t {bearer token} -m {mount dir}
```

For Windows:

```
java -Dfile.encoding=UTF-8 -jar ega-fuse-0.1.0.jar -t {bearer token} -m {mount dir e.g, Z:\}
```

optional arguments:
  -url_token
  -url_auth
  -url_base
  -url_cega

This will populate the {mount dir} with a directory called "Datasets". To view all datasets to which the OAuth2 token provides access:

```
cd Datasets
ls
```

Running 'ls' or 'll' will populate the directory, otherwise the Datasets directory will be empty (it is not possible to directly cd into a Dataset directory upon starting the FUSE client, because it will have to be populated first).

The same procedure then applies to Dataset directories. Change into a dataset, then run 'ls' or 'll' to populate the directory. Now all files listed in the directory can be used like norman file system files.

## Starting the FUSE layer with Refresh Tokens

OAuth2 Bearer tokens have a validity of 1 hour. For tasks lasting longer than one hour a refresh token can be specified; in this case a config file (e.g. config.ini) must also be provided with specifications on how to contact the AAI. The file config.ini is provided here in the project and it is same for everyone.

The FUSE layer is then started as:

```
java -jar ega-fuse-0.1.0.jar -t {bearer token} -rt {refresh token} -m {mount dir} -f config.ini
```

FUSE layer can also run using username and password as:

```
java -jar ega-fuse-0.1.0.jar -u {username} -p {password} -m {mount dir} -f config.ini
```

## Starting the FUSE layer in background with a script
The fuse layer can also be started, restarted and stopped using shell script ./fuseclient.sh as:

```
 ./fuseclient.sh start "-t {bearer token} -m {mount dir} -f config.ini"
```
 
```
  ./fuseclient.sh restart "-t {bearer token} -m {mount dir} -f config.ini"
```

``` 
  ./fuseclient.sh stop
```
### Troubleshoot fuseclient.sh
Check the log file fuse-client-logs.log, If you see any error as /tmp/mnt can not be used as mount point. Try running below command

```
umount -l /tmp/mnt
```

Note: Change /tmp/mnt to your mount point path 

### Generate the Bearer token & Refresh token
To get the bearer token use below command. Replace your username and password:

```
curl -k --data "grant_type=password&client_id=f20cd2d3-682a-4568-a53e-4262ef54c8f4&client_secret=AMenuDLjVdVo4BSwi0QD54LL6NeVDEZRzEQUJ7hJOM3g4imDZBHHX0hNfKHPeQIGkskhtCmqAJtt_jm7EKq-rWw&username={USERNAME}&password={PASSWORD}&scope=openid" https://ega.ebi.ac.uk:8443/ega-openid-connect-server/token
```

This produces a JSON response containing these elements `{"access_token":"###","token_type":"Bearer","expires_in":3599,"scope":"openid","id_token":"###"}`.
Here the value of access_token is the bearer token.

To get the refresh token use below command. Replace your username and password:

```
curl -k --data "grant_type=password&client_id=f20cd2d3-682a-4568-a53e-4262ef54c8f4&client_secret=AMenuDLjVdVo4BSwi0QD54LL6NeVDEZRzEQUJ7hJOM3g4imDZBHHX0hNfKHPeQIGkskhtCmqAJtt_jm7EKq-rWw&username={USERNAME}&password={PASSWORD}&scope=openid offline_access" https://ega.ebi.ac.uk:8443/ega-openid-connect-server/token
```

This produces a JSON response containing these elements `{"access_token":"###","token_type":"Bearer", "refresh_token": "###","expires_in":3599,"scope":"openid offline_access","id_token":"###"}`.
Here the value of access_token is the bearer token. The “refresh_token” is optionally necessary if it is expected to run for longer than 1 hour)

## License

This project is licensed under the Apache 2.0 License - see the [LICENSE.md](LICENSE.md) file for details

