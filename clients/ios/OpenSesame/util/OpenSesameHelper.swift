import Foundation

class OpenSesameHelper {

    // Helper to group conversations by Today, Yesterday, and Previous
    static func getGroupedConversations (conversations: [ConversationModel]) -> [String: [ConversationModel]] {
        var grouped = ["Today": [ConversationModel](), "Yesterday": [ConversationModel](), "Previous": [ConversationModel]()]

        let calendar = Calendar.current
        for conversation in conversations {
            if calendar.isDateInToday(conversation.updatedAt) {
                grouped["Today"]?.append(conversation)
            } else if calendar.isDateInYesterday(conversation.updatedAt) {
                grouped["Yesterday"]?.append(conversation)
            } else {
                grouped["Previous"]?.append(conversation)
            }
        }

        return grouped
    }

    static func parseStringAsDate(dateAsString: String) -> Date? {
        let dateFormatter = DateFormatter()
        dateFormatter.dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSZ"
        dateFormatter.locale = Locale(identifier: "en_US_POSIX")
        dateFormatter.timeZone = TimeZone(secondsFromGMT: 0)
        return dateFormatter.date(from: dateAsString)
    }

}
