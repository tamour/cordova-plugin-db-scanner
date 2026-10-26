import Foundation

@objc(CDVDatabaseScanner)
class CDVDatabaseScanner: CDVPlugin {

    @objc(listDatabases:)
    func listDatabases(command: CDVInvokedUrlCommand) {
        commandDelegate.run(inBackground: {
            var dbList: [[String: Any]] = []
            let fileManager = FileManager.default

            // Directories where Cordova, OutSystems, and SQLite plugins keep DB files
            let searchPaths: [URL] = [
                fileManager.urls(for: .documentDirectory, in: .userDomainMask).first,
                fileManager.urls(for: .libraryDirectory, in: .userDomainMask).first
            ].compactMap { $0 }

            for baseDir in searchPaths {
                guard let enumerator = fileManager.enumerator(
                    at: baseDir,
                    includingPropertiesForKeys: [.fileSizeKey, .isDirectoryKey],
                    options: [.skipsHiddenFiles]
                ) else { continue }

                for case let fileURL as URL in enumerator {
                    do {
                        let resourceValues = try fileURL.resourceValues(forKeys: [.isDirectoryKey, .fileSizeKey])
                        if resourceValues.isDirectory == false && self.isSQLiteFile(at: fileURL) {
                            let item: [String: Any] = [
                                "name": fileURL.lastPathComponent,
                                "fullPath": fileURL.path,
                                "size": resourceValues.fileSize ?? 0
                            ]
                            dbList.append(item)
                        }
                    } catch {
                        continue
                    }
                }
            }

            let pluginResult = CDVPluginResult(status: CDVCommandStatus_OK, messageAs: dbList)
            self.commandDelegate.send(pluginResult, callbackId: command.callbackId)
        })
    }

    // Inspects the first 16 bytes for the standard SQLite file magic header: "SQLite format 3\0"
    private func isSQLiteFile(at url: URL) -> Bool {
        guard let handle = try? FileHandle(forReadingFrom: url) else { return false }
        defer { try? handle.close() }

        guard let headerData = try? handle.read(upToCount: 16), headerData.count >= 16 else {
            return false
        }

        let sqliteMagic = "SQLite format 3\0".data(using: .utf8)!
        return headerData.prefix(sqliteMagic.count) == sqliteMagic
    }
}