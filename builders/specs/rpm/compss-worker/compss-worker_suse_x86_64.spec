%define name	 	compss-worker
%define version		2.5
%define release		1

Requires: java-1_8_0-openjdk
Summary: The BSC COMP Superscalar Runtime Worker
Name: %{name}
Version: %{version}
Release: %{release}
License: Apache 2.0
Group: Development/Libraries
Source: %{name}-%{version}.tar.gz
Distribution: Linux
Vendor: Barcelona Supercomputing Center - Centro Nacional de Supercomputación
URL: http://compss.bsc.es
Packager: COMPSs Support <support-compss@bsc.es>
Prefix: /opt
ExclusiveArch: x86_64

%description
The BSC COMP Superscalar Runtime Worker.

%prep
%setup -q

#------------------------------------------------------------------------------------
%build
echo "* Building COMP Superscalar Runtime Worker..."
echo " "

echo "   - Compile sources"
cd adaptors/
mvn -U clean install
cd ../

echo "   - Create deployment folders"
mkdir -p COMPSs
mkdir -p COMPSs/Runtime/scripts
mkdir -p COMPSs/Runtime/adaptors

echo "   - Copy deployment files"
#Doc
cp changelog COMPSs/
cp LICENSE COMPSs/
cp NOTICE COMPSs/
cp README COMPSs/
cp RELEASE_NOTES COMPSs/
#Scripts
cp -r scripts/* COMPSs/Runtime/scripts/


#Adaptors
find adaptors -name pom.xml | xargs rm -rf
rm -r adaptors/commons
path_source="adaptors"
path_target="COMPSs/Runtime/adaptors"
adaptors=$(ls ${path_source})
for adaptor in $adaptors; do
  if [ "$adaptor" != "agent" ]; then
    #Regular adaptor: Copying only worker part
    mkdir ${path_target}/$adaptor
    mkdir ${path_target}/$adaptor/worker
    #Installing worker jars and properties
    cp ${path_source}/$adaptor/worker/*.jar ${path_target}/$adaptor/worker
    if [ -f ${path_source}/$adaptor/worker/properties ]; then
      cp ${path_source}/$adaptor/worker/properties ${path_target}/$adaptor/worker
    fi
    #Installing scripts
    if [ -d "${path_source}/$adaptor/scripts/" ]; then
      mkdir -p ${COMPSs_target}/Runtime/scripts/system/adaptors/$adaptor/
      cp -r ${path_source}/$adaptor/scripts/* ${COMPSs_target}/Runtime/scripts/system/adaptors/$adaptor/
    fi
  else
    # Agents adaptors
    agent_path_source=${path_source}/${adaptor}
    agents=$(ls ${agent_path_source})
    for agent in $agents; do
      agent_name=$(basename "${agent}")
      if [ "${agent_name}" != "target" ] && [ "${agent_name}" != "pom.xml" ]; then
        #Agent: Copying
        #   - worker (necessary for being published as an agent)
        mkdir ${path_target}/$agent
        #Installing worker jars and properties
        if [ -d "${agent_path_source}/$agent/worker/" ]; then
          mkdir ${path_target}/$agent/worker
          cp ${agent_path_source}/$agent/worker/*.jar ${path_target}/$agent/worker
          if [ -f ${agent_path_source}/$agent/worker/properties ]; then
            cp ${agent_path_source}/$agent/worker/properties ${path_target}/$agent/worker
          fi
        fi
        #Installing scripts
        if [ -d "${agent_path_source}/$agent/scripts/" ]; then
          mkdir -p ${COMPSs_target}/Runtime/scripts/system/adaptors/$agent/
          cp -r ${agent_path_source}/$agent/scripts/* ${COMPSs_target}/Runtime/scripts/system/adaptors/$agent/
        fi
      fi
    done
  fi
done


echo "   - Erase sources"
ls . | grep -v COMPSs | xargs rm -r

echo "COMP Superscalar Runtime Engine built"
echo " "

#------------------------------------------------------------------------------------
%install
echo "* Installing COMPSs Runtime..."

echo " - Checking if COMPSs Runtime Engine is installed"
if [ ! -d "${RPM_BUILD_ROOT}/opt/COMPSs" ]; then
   echo " - No COMPSs dependencies installed."
   echo " - Creating only Worker machine installation"
   mkdir -p ${RPM_BUILD_ROOT}/opt/
   cp -r COMPSs ${RPM_BUILD_ROOT}/opt/
else
   echo " - COMPSs dependencies installed"
   echo " - Creating master/worker machine installation"
   source_base_path=COMPSs/Runtime/scripts/adaptors
   target_base_path=${RPM_BUILD_ROOT}/opt/COMPSs/Runtime/scripts/system/adaptors
   mkdir -p ${target_base_path}
   adaptor_scripts=$(ls ${source_base_path})
   for $adaptor in ${adaptor_scripts}; do
     mkdir -p ${target_base_path}/$adaptor/
     cp -r ${source_base_path}/$adaptor/* ${target_base_path}/$adaptor/
   done
   mkdir -p ${RPM_BUILD_ROOT}/opt/COMPSs/Runtime/adaptors
   source_base_path=COMPSs/Runtime/adaptors
   target_base_path=${RPM_BUILD_ROOT}/opt/COMPSs/Runtime/adaptors
   mkdir -p ${target_base_path}
   adaptor_jars=$(ls ${source_base_path})
   for $adaptor in ${adaptor_jars}; do
     mkdir -p ${target_base_path}/$adaptor/
     cp -r ${source_base_path}/$adaptor/* ${target_base_path}/$adaptor/
   done
fi

echo " - COMPSs Runtime Worker structure created"
echo " "

echo " - Setting COMPSs Runtime Worker permissions..."
chmod 755 -R ${RPM_BUILD_ROOT}/opt/COMPSs/
echo " - COMPSs Runtime Worker permissions set"
echo " "

echo "Congratulations!"
echo "COMPSs Runtime Worker Successfully installed!"
echo " "

#------------------------------------------------------------------------------------
%post 
echo "* Installing COMPSs Runtime Worker..."
echo " "

echo "Congratulations!"
echo "COMPSs Runtime Worker Successfully installed!"
echo " "


#------------------------------------------------------------------------------------
%preun

#------------------------------------------------------------------------------------
%postun 
rm -rf /opt/COMPSs/
echo "COMPSs Runtime Worker Successfully uninstalled!"
echo " "

#------------------------------------------------------------------------------------
%clean
rm -rf ${RPM_BUILD_ROOT}/opt/COMPSs/

#------------------------------------------------------------------------------------
%files 
%defattr(-,root,root)
/opt/COMPSs/

