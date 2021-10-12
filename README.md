# Devoxx

**From developers, For developers**

[Devoxx](https://beta.devoxx.com/) and [Voxxed Days](https://beta.voxxeddays.com) are a set of international conferences.

[![License](https://img.shields.io/badge/License-BSD%203--Clause-blue.svg)](https://opensource.org/licenses/BSD-3-Clause)

This repository contains the code for the official Devoxx app, based on [Gluon Mobile](http://gluonhq.com/products/mobile/),
for Android and iOS devices.

## Features

The app includes the following set of features:

* Sessions catalogue
* Session description
* Speakers list
* Speaker description
* Sponsors list
* Scan badge using QR scanner
* Venue description and location on map
* Vote on sessions after completion
* Favorite sessions to get notification before session starts
* Take notes based on each session

Source code is released as a sample of a professional Gluon Mobile application and therefore makes use of best practices.

Although these features are tailor made for Devoxx, other conferences
can re-use most of the features. We encourage them to fork and use the source code.
Please refer to our [Wiki](https://github.com/devoxx/MyDevoxxGluon/wiki)
for complete description of the project and how to start development.

## Getting Started

Before you clone and run the project, check the GluonFX plugin [documentation](https://docs.gluonhq.com/).

For more details about the project, please refer to our [Wiki](https://github.com/devoxx/MyDevoxxGluon/wiki).

Once you have cloned the project, with JDK 11, build it from the root:

    mvn clean install

Then cd into the `DevoxxClientMobile` folder. 

### Desktop

Run the application on JVM/HotSpot:

    mvn gluonfx:run

Build a native image using:

    mvn gluonfx:build

Run the native image app:

    mvn gluonfx:nativerun

### Android

Build a native image for Android using:

    mvn gluonfx:build -Pandroid

Package the native image as an 'apk' file:

    mvn gluonfx:package -Pandroid

Install it on a connected android device:

    mvn gluonfx:install -Pandroid

Run the installed app on a connected android device:

    mvn gluonfx:nativerun -Pandroid

### iOS

Build a native image for iOS using:

    mvn gluonfx:build -Pios

Install and run the native image on a connected iOS device:

    mvn gluonfx:nativerun -Pios

Create an IPA file (for submission to TestFlight or App Store):

    mvn gluonfx:package -Pios

## Contributing

We appreciate your help!

We use [github issue tracker](https://github.com/devoxx/MyDevoxxGluon/issues)
for all bug reports and proposals.

## Support

If you have any questions, please reach out to us at [Gluon support](http://www.gluonhq.com/support).

Gluon offers custom consulting services to develop cross-platform mobile applications.
If you want to learn more, our services are detailed at http://www.gluonhq.com/services.

This source is released under the BSD license and is copyright by Gluon.
