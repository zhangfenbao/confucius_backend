import Foundation

struct ConversationCreateModel: Codable {
    let workspaceID: UUID
    let title: String?
    let languageCode: String?

    enum CodingKeys: String, CodingKey {
        case workspaceID = "workspace_id"
        case title
        case languageCode = "language_code"
    }
}

struct ConversationModel: Codable, Identifiable {
    var id: UUID {
        conversationID
    }
    let conversationID: UUID
    let workspaceID: UUID
    let title: String?
    let archived: Bool?
    let languageCode: String?
    let createdAt: Date
    let updatedAt: Date

    enum CodingKeys: String, CodingKey {
        case conversationID = "conversation_id"
        case workspaceID = "workspace_id"
        case title, archived
        case languageCode = "language_code"
        case createdAt = "created_at"
        case updatedAt = "updated_at"
    }

    init(conversationID: UUID, workspaceID: UUID, title: String?, archived: Bool?, languageCode: String?, createdAt: Date, updatedAt: Date) {
        self.conversationID = conversationID
        self.workspaceID = workspaceID
        self.title = title
        self.archived = archived
        self.languageCode = languageCode
        self.createdAt = createdAt
        self.updatedAt = updatedAt
    }

    init(from decoder: any Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        self.conversationID = try container.decode(UUID.self, forKey: .conversationID)
        self.workspaceID = try container.decode(UUID.self, forKey: .workspaceID)
        self.title = try container.decodeIfPresent(String.self, forKey: .title)
        self.archived = try container.decodeIfPresent(Bool.self, forKey: .archived)
        self.languageCode = try container.decodeIfPresent(String.self, forKey: .languageCode)

        let createdAtString = try container.decode(String.self, forKey: .createdAt)
        guard let createdAtDate = OpenSesameHelper.parseStringAsDate(dateAsString: createdAtString) else {
            throw DecodingError.dataCorruptedError(forKey: .createdAt, in: container, debugDescription: "Date string does not match format expected by formatter.")
        }
        self.createdAt = createdAtDate

        let updatedAtString = try container.decode(String.self, forKey: .updatedAt)
        guard let updatedAtDate = OpenSesameHelper.parseStringAsDate(dateAsString: updatedAtString) else {
            throw DecodingError.dataCorruptedError(forKey: .updatedAt, in: container, debugDescription: "Date string does not match format expected by formatter.")
        }
        self.updatedAt = updatedAtDate

    }
}

struct ConversationMessagesModel: Codable {
    let conversation: ConversationModel
    let messages: [MessageModel]
}

