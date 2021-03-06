#!/bin/bash -e

  ROOT_FOLDER=/
  SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

  # Clean apt
  echo "Cleaning APT-GET"
  sudo apt-get -y --force-Yes update
  sudo apt-get -y --force-Yes autoremove
  sudo apt-get -y --force-Yes autoclean
  sudo apt-get -y --force-Yes purge
  sudo apt-get -y --force-Yes clean

  # Clean subversion
  echo "Cleanning GIT/MAVEN/COMPSs files"
  cd "$ROOT_FOLDER"
  sudo find . -depth -name ".git*"| xargs -r -i -t rm -rf {}
  sudo find . -name ".m2" | xargs -r -i -t rm -rf {}
  sudo find . -name ".COMPSs" | xargs -r -i -t rm -rf {}

  # Add 0's to image 
  cd "$SCRIPT_DIR"
  echo "Adding 0's"
  ./clean_0s.sh

  # End
  echo "DONE!"
  exit 0

