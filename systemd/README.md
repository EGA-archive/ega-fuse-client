# Example files for running the EGA Fuse client as a service using systemd

## ega-fuse-client@.service

A systemd service file that allows running multiple instances of the EGA Fuse client for different mount points.
This file must be located in
```
/usr/lib/systemd/system/ega-fuse-client@.service
```
Each instance requires:
 * An instancename
 * An empty dir to serve as mount point.
 * Two configuration files (see below for details)
   * located in ```/etc/ega-fuse-client.d/```
   * and named ```instancename.cf``` and ```instancename.env```
 * A symlink ```/etc/systemd/system/multi-user.target.wants/ega-fuse-client@instancename.service -> /usr/lib/systemd/system/ega-fuse-client@.service```
   This symlink will be created with
   ```
   systemctl enable ega-fuse-client@instancename.service
   ```

When everything is in place, you can start an instance with:
```
systemctl start ega-fuse-client@instancename.service
```

## ega-fuse-client.cf.example

Example of a **c**redential **f**ile. This contains the username and password that will be used to authenticate at the EGA.  
This configuration file must be located at
```
/etc/ega-fuse-client.d/instancename.cf
```

## ega-fuse-client.env.example

Example of an **env**ironment file. Here we define environment variables used by the systemd service file to start the EGA fuse client as a service.  

 * ```EGA_FUSE_CLIENT_MOUNTPOINT```: the mount point to be used for the instance.
 * ```EGA_FUSE_CLIENT_JAVA_HOME```: the path to the JAVA_HOME for the Java version to be used with the instance.

This configuration file must be located at
```
/etc/ega-fuse-client.d/instancename.env
```