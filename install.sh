#!/bin/bash
PREFIX=/usr/local
INSTDIR=$PREFIX/etc/converge
DBDIR=/var/lib/converge
SCRIPTDIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

mkdir -p $DBDIR
mkdir -p $HOME/.CertShim

mkdir -p $INSTDIR/notaries-available
mkdir -p $INSTDIR/notaries-enabled

cp $SCRIPTDIR/lib/converge.config $INSTDIR
cp $SCRIPTDIR/lib/*.notary $INSTDIR/notaries-available

for n in $(ls $INSTDIR/notaries-available)
do
	ln -f -s $INSTDIR/notaries-available/$n $INSTDIR/notaries-enabled/$n
done


sqlite3 $DBDIR/converge.db < $SCRIPTDIR/lib/converge.sql
sqlite3 $DBDIR/cache.db < $SCRIPTDIR/lib/cache.sql

sqlite3 $HOME/.CertShim/converge.db < $SCRIPTDIR/lib/converge.sql
sqlite3 $HOME/.CertShim/cache.db < $SCRIPTDIR/lib/cache.sql

cd $SCRIPTDIR
javac org/CertShim/*;
jar cf libs.jar org/CertShim/CertShimMain.class org/CertShim/CheckThread.class org/CertShim/JConverge*.class org/CertShim/SSLCheckable.class
mv libs.jar $HOME/.CertShim
mv lib/*.jar $HOME/.CertShim
jar cfm ag.jar man org/CertShim/Agent.class org/CertShim/CertShimTrans.class
mv ag.jar $HOME/.CertShim

