export function cleanMarkdown(markdown: string) {
  // Remove code blocks (``` or ~~~)
  markdown = markdown.replace(/```[\s\S]*?```|~~~[\s\S]*?~~~/g, "");

  // Remove inline code (`code`)
  markdown = markdown.replace(/`[^`]*`/g, "");

  // Remove headings (e.g., ### Heading)
  markdown = markdown.replace(/(^|\n)#+\s*/g, "");

  // Remove blockquotes (e.g., > Quote)
  markdown = markdown.replace(/(^|\n)>\s*/g, "");

  // Remove emphasis (*text* or _text_)
  markdown = markdown.replace(/(\*|_){1,3}(.+?)\1/g, "$2");

  // Remove links [text](url)
  markdown = markdown.replace(/\[([^\]]+)\]\([^\)]+\)/g, "$1");

  // Remove images ![alt text](url)
  markdown = markdown.replace(/!\[([^\]]*)\]\([^\)]+\)/g, "");

  // Remove bold (**text** or __text__)
  markdown = markdown.replace(/(\*\*|__)(.*?)\1/g, "$2");

  // Remove strikethroughs (~~text~~)
  markdown = markdown.replace(/~~(.*?)~~/g, "$1");

  // Remove any remaining special characters related to Markdown formatting
  markdown = markdown.replace(/[*_~`>]/g, "");

  // Trim excess whitespace
  return markdown.trim();
}
