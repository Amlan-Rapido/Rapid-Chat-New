import SwiftUI
import chatKit

@main
struct iOSApp: App {

    init(){
    KoinIOSKt.doInitKoin { koinApplication in }
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}