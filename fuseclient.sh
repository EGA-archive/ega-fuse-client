SERVICE_NAME="Fuse client"
JAR_NAME="ega-fuse-0.0.1-SNAPSHOT.jar"
PATH_TO_JAR="target/$JAR_NAME"
PROCESSCNT=$(ps x |grep -v grep |grep -c "$JAR_NAME")
PID=$(ps aux | grep "$JAR_NAME" | grep -v grep | awk '{print $2}')
ARGS=$2

if ! [[ $ARGS == *"-cf"* ]] && [[ "$1" == "start" || "$1" == "restart" ]]  ; then

CRED_FILE="$PWD/cred_ega_fuse.tmp"
touch $CRED_FILE
chmod 600 $CRED_FILE

printf "Enter username:"
while read username; do
        if [ -z $username ]; then
                printf "\nNo input entered! Enter username:"
        else
                break
        fi
done

printf "Enter password:"
while read -s password; do
        if [ -z $password ]; then
                printf "\nNo input entered! Enter password:"
        else
                break
        fi
done

echo "username:$username" > $CRED_FILE
echo "password:$password" >> $CRED_FILE

ARGS="$ARGS --cf=$CRED_FILE"
fi
        
case $1 in
    start)
        echo "Starting $SERVICE_NAME ..."
        if [ $PROCESSCNT == 0 ]; then
            nohup java -jar $PATH_TO_JAR $ARGS >> fuse-client-logs.log &
            echo "$SERVICE_NAME started ..."
        else
            echo "$SERVICE_NAME is already running ..."
        fi
    ;;
    stop)
        if [ $PROCESSCNT != 0 ]; then
            echo "$SERVICE_NAME stopping ..."
            kill -9 $PID;
                        sleep 2s
            echo "$SERVICE_NAME stopped ..."
        else
            echo "$SERVICE_NAME is not running ..."
        fi
    ;;
    restart)
        if [ $PROCESSCNT == 0 ]; then
            echo "$SERVICE_NAME stopping ...";
            kill -9 $PID;
                        sleep 2s
            echo "$SERVICE_NAME stopped ...";
        else
			echo "$SERVICE_NAME is not running ..."
		fi
        echo "$SERVICE_NAME starting ..."
        nohup java -jar $PATH_TO_JAR $ARGS >>  fuse-client-logs.log &
        echo "$SERVICE_NAME started ..."
    ;;
esac
