How to generate a default keystore
----------------------------------

Create your key following the instructions at [http://developer.android.com/tools/publishing/app-signing.html#cert](http://developer.android.com/tools/publishing/app-signing.html#cert)

Example:

>       keytool -genkey \
              -alias androiddebugkey \
              -keyalg RSA \
              -dname 'CN=Grimault Sebastien, O=Makina Corpus, L=Nantes, ST=France, C=FR' \
              -storepass android \
              -keystore debug.keystore \
              -keypass android \
              -startdate '2014/01/03 12:00:00' \
              -validity 686

To print details:

>       keytool -v \
              -list \
              -keystore debug.keystore \
              -alias androiddebugkey \
              -storepass android \
              -keypass android

Get the app's certificate information
-------------------------------------

SHA-1 fingerprint:

>       keytool -v \
              -list \
              -keystore debug.keystore \
              -alias androiddebugkey \
              -storepass android \
              -keypass android \
              | grep SHA1 | head -n 1 | cut -d" " -f4