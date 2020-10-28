/** 
MIT License

Copyright (c) 2019 mengtnt

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/

import Foundation
import UIKit
import CommonCrypto

extension String {
    func md5() -> String {
        let str = self.cString(using: String.Encoding.utf8)
        let strLen = CUnsignedInt(self.lengthOfBytes(using: String.Encoding.utf8))
        let digestLen = Int(CC_MD5_DIGEST_LENGTH)
        let result = UnsafeMutablePointer<UInt8>.allocate(capacity: 16)
        CC_MD5(str!, strLen, result)
        let hash = NSMutableString()
        for i in 0 ..< digestLen {
            hash.appendFormat("%02x", result[i])
        }
        free(result)
        return String(format: hash as String)
    }
}

public class FileStorage {
    
    public static let share:FileStorage? = {
        return try? FileStorage(subDirName:"ExportImage")
    }()
    
    let fileManager: FileManager
    let path: String
    
    init(subDirName:String,mainDirURL:URL? = nil) throws {
        let url: URL
        self.fileManager = FileManager.default
        if let hasUrl = mainDirURL {
            url = hasUrl
        }
        else{
            url = try fileManager.url(
                for: .documentDirectory, in: .userDomainMask, appropriateFor: nil, create: true
            )
        }
        
        path = url.appendingPathComponent(subDirName, isDirectory: true).path
        try createDirectory()
    }
    
    init(dirName:String) throws {
        self.fileManager = FileManager.default
        path = URL.init(fileURLWithPath: dirName).path
        try createDirectory()
    }
    
    func createDirectory() throws {
        guard !fileManager.fileExists(atPath: path) else {
            return
        }
        try fileManager.createDirectory(atPath: path, withIntermediateDirectories: true,
                                        attributes: nil)
    }
    
    func createFile(byPath newPath:String, content:Data?) -> Bool {
        _ = removeFile(newPath)
        let result = fileManager.createFile(atPath: newPath, contents: content, attributes: nil)
        return result
    }
    
    func fileName(for key: String) -> String {
        return key.md5()
    }
    
    func filePath(for key: String, suffix: String?) -> String {
        return "\(path)/\(fileName(for: key))\(suffix ?? "")"
    }
    
    func filePath(forName fileName: String) -> String {
        return "\(path)/\(fileName)"
    }
    
    public func searchFile(for key:String) -> URL? {
        if fileManager.fileExists(atPath: filePath(for: key, suffix: nil)){
            return URL(fileURLWithPath: filePath(for: key, suffix: nil))
        }
        return nil
    }
    
    func cleanAllFile() throws {
        let contents = try fileManager.contentsOfDirectory(atPath: path)
        for pathComponent in contents {
            let filePath = NSString(string: path).appendingPathComponent(pathComponent)
            let url = URL(fileURLWithPath: filePath)
            try fileManager.removeItem(at: url)
        }
    }
    
    func removeFile(_ filePath:String) -> Bool {
        let url = URL(fileURLWithPath: filePath)
        do {
            try fileManager.removeItem(at: url)
            return true
        } catch {
            return false
        }
    }
    
    func fileSize(path:String) -> UInt64 {
        let attributes = try? fileManager.attributesOfItem(atPath: path)
        if let fileSize = attributes?[.size] as? UInt64 {
            return fileSize
        }
        return 0
    }
    
    func fileSize(key:String) -> UInt64 {
        let path = filePath(for: key, suffix: nil)
        return fileSize(path:path)
    }
    
}
