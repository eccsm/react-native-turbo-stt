require "json"

package = JSON.parse(File.read(File.join(__dir__, "package.json")))

Pod::Spec.new do |s|
  s.name         = "TurboStt"
  s.version      = package["version"]
  s.summary      = package["description"]
  s.homepage     = package["homepage"]
  s.license      = package["license"]
  s.authors      = package["author"]

  s.platforms    = { :ios => "12.4" }
  
  s.source       = { :git => "https://github.com/eccsm/react-native-turbo-stt.git", :tag => "#{s.version}" }

  s.source_files = "ios/**/*.{h,m,mm,swift}"

  s.swift_version = "5.0"

  s.pod_target_xcconfig = {
    "DEFINES_MODULE" => "YES"
  }

  install_modules_dependencies(s)
end