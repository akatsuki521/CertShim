#!/bin/bash
PREFIX=/usr/local
INSTDIR=$PREFIX/etc/converge
DBDIR=/var/lib/converge
SCRIPTDIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

sudo mkdir -p $DBDIR
mkdir -p $HOME/.CertShim

sudo mkdir -p $INSTDIR/notaries-available
sudo mkdir -p $INSTDIR/notaries-enabled

sudo cp $SCRIPTDIR/lib/converge.config $INSTDIR
sudo cp $SCRIPTDIR/lib/*.notary $INSTDIR/notaries-available

for n in $(ls $INSTDIR/notaries-available)
do
	sudo ln -f -s $INSTDIR/notaries-available/$n $INSTDIR/notaries-enabled/$n
done


sudo sqlite3 $DBDIR/converge.db < $SCRIPTDIR/lib/converge.sql
sudo sqlite3 $DBDIR/cache.db < $SCRIPTDIR/lib/cache.sql

sqlite3 $HOME/.CertShim/converge.db < $SCRIPTDIR/lib/converge.sql
sqlite3 $HOME/.CertShim/cache.db < $SCRIPTDIR/lib/cache.sql

cd $SCRIPTDIR
javac -cp lib/*:. org/CertShim/*.java;
jar cf libs.jar org/CertShim/CertShimMain.class org/CertShim/CheckThread.class org/CertShim/JConverge*.class org/CertShim/SSLCheckable.class
mv libs.jar $HOME/.CertShim
cp lib/*.jar $HOME/.CertShim
jar cfm ag.jar man org/CertShim/Agent.class org/CertShim/CertShimTrans.class
mv ag.jar $HOME/.CertShim

