cdir=`pwd`
cd /tmp/abc
rm src.zip
rm -rf src
mv ../src.zip .
unzip -x src.zip
cd $cdir
rm -rf src
mv /tmp/abc/src .
