package model;

import org.fife.ui.rsyntaxtextarea.*;

import javax.swing.text.Segment;

public class TokenMaker extends AbstractTokenMaker {


	@Override
	public TokenMap getWordsToHighlight() {
		TokenMap tokenMap = new TokenMap(true);
		tokenMap.put("WORD",  Token.RESERVED_WORD);
		tokenMap.put("(SP)", Token.MARKUP_CDATA);

		for(String instruction : AssemblerBefehle.getAssemblyInstructions().getMnemonics()) {
			tokenMap.put(instruction, Token.FUNCTION);
		}

		return tokenMap;
	}

	@Override
	public void addToken(Segment segment, int start, int end, int tokenType, int startOffset) {
		// This assumes all keywords, etc. were parsed as "identifiers."
		if (tokenType==Token.IDENTIFIER) {
			int type = wordsToHighlight.get(segment, start, end);
			if (type != -1) {
				tokenType = type;
			}
		}
		super.addToken(segment, start, end, tokenType, startOffset);
	}

	@Override
	public Token getTokenList(Segment text, int startTokenType, int startOffset) {

		resetTokenList();

		char[] array = text.array;
		int offset = text.offset;
		int count = text.count;
		int end = offset + count;

		// Token starting offsets are always of the form:
		// 'startOffset + (currentTokenStart-offset)', but since startOffset and
		// offset are constant, tokens' starting positions become:
		// 'newStartOffset+currentTokenStart'.
		int newStartOffset = startOffset - offset;

		int currentTokenStart = offset;
		int currentTokenType  = startTokenType;

		for (int i = offset; i < end; i++) {

			char curChar = array[i];

			switch (currentTokenType) {

				case Token.NULL:

					currentTokenStart = i;   // Starting a new token here.

					switch (curChar) {

						case ' ':
						case '\t':
							currentTokenType = Token.WHITESPACE;
							break;

						case '#':
							currentTokenType = Token.COMMENT_EOL;
							break;

						default:
							if (RSyntaxUtilities.isDigit(curChar)) {
								currentTokenType = Token.LITERAL_NUMBER_DECIMAL_INT;
								break;
							}
							else if (RSyntaxUtilities.isLetter(curChar) || curChar=='/' || curChar=='_') {
								currentTokenType = Token.IDENTIFIER;
								break;
							}

							// Anything not currently handled - mark as an identifier
							currentTokenType = Token.IDENTIFIER;
							break;

					} // End of switch (c).

					break;

				case Token.WHITESPACE:

					switch (curChar) {

						case ' ':
						case '\t':
							break;   // Still whitespace.

						case '#':
							addToken(text, currentTokenStart,i-1, Token.WHITESPACE, newStartOffset+currentTokenStart);
							currentTokenStart = i;
							currentTokenType = Token.COMMENT_EOL;
							break;

						default:   // Add the whitespace token and start anew.

							addToken(text, currentTokenStart,i-1, Token.WHITESPACE, newStartOffset+currentTokenStart);
							currentTokenStart = i;

							if (RSyntaxUtilities.isDigit(curChar)) {
								currentTokenType = Token.LITERAL_NUMBER_DECIMAL_INT;
								break;
							}
							else if (RSyntaxUtilities.isLetter(curChar) || curChar=='/' || curChar=='_') {
								currentTokenType = Token.IDENTIFIER;
								break;
							}

							// Anything not currently handled - mark as identifier
							currentTokenType = Token.IDENTIFIER;

					} // End of switch (c).

					break;

				default: // Should never happen
				case Token.IDENTIFIER:

					switch (curChar) {

						case ' ':
						case '\t':
							addToken(text, currentTokenStart,i-1, Token.IDENTIFIER, newStartOffset+currentTokenStart);
							currentTokenStart = i;
							currentTokenType = Token.WHITESPACE;
							break;


						default:
							if (RSyntaxUtilities.isLetterOrDigit(curChar) || curChar=='/' || curChar=='_') {
								break;   // Still an identifier of some type.
							}
							// Otherwise, we're still an identifier (?).

					} // End of switch (c).

					break;

				case Token.LITERAL_NUMBER_DECIMAL_INT:

					switch (curChar) {

						case ' ':
						case '\t':
							addToken(text, currentTokenStart,i-1, Token.LITERAL_NUMBER_DECIMAL_INT, newStartOffset+currentTokenStart);
							currentTokenStart = i;
							currentTokenType = Token.WHITESPACE;
							break;

						case '"':
							addToken(text, currentTokenStart,i-1, Token.LITERAL_NUMBER_DECIMAL_INT, newStartOffset+currentTokenStart);
							currentTokenStart = i;
							currentTokenType = Token.LITERAL_STRING_DOUBLE_QUOTE;
							break;

						case 'x':
							if(array[i-1] == '0') {
								addToken(text, currentTokenStart,i-1, Token.LITERAL_NUMBER_HEXADECIMAL, newStartOffset+currentTokenStart);
								currentTokenStart = i;
								currentTokenType = Token.LITERAL_NUMBER_HEXADECIMAL;
								break;
							}

						default:

							if (RSyntaxUtilities.isDigit(curChar)) {
								break;   // Still a literal number.
							}

							// Otherwise, remember this was a number and start over.
							addToken(text, currentTokenStart,i-1, Token.LITERAL_NUMBER_DECIMAL_INT, newStartOffset+currentTokenStart);
							i--;
							currentTokenType = Token.NULL;

					} // End of switch (c).

					break;

				case Token.COMMENT_EOL:
					i = end - 1;
					addToken(text, currentTokenStart,i, currentTokenType, newStartOffset+currentTokenStart);
					// We need to set token type to null so at the bottom we don't add one more token.
					currentTokenType = Token.NULL;
					break;


				case Token.LITERAL_NUMBER_HEXADECIMAL:
					switch (curChar) {

						case ' ':
						case '\t':
							addToken(text, currentTokenStart,i-1, Token.LITERAL_NUMBER_HEXADECIMAL, newStartOffset+currentTokenStart);
							currentTokenStart = i;
							currentTokenType = Token.WHITESPACE;
							break;


						default:

							if (RSyntaxUtilities.isDigit(curChar)) {
								break;   // Still a literal number.
							}

							// Otherwise, remember this was a number and start over.
							addToken(text, currentTokenStart,i-1, Token.LITERAL_NUMBER_HEXADECIMAL, newStartOffset+currentTokenStart);
							i--;
							currentTokenType = Token.NULL;

					} // End of switch (c).

					break;

			} // End of switch (currentTokenType).

		} // End of for (int i=offset; i<end; i++).

		switch (currentTokenType) {

			// Remember what token type to begin the next line with.
			//case Token.LITERAL_STRING_DOUBLE_QUOTE:
			//	addToken(text, currentTokenStart,end-1, currentTokenType, newStartOffset+currentTokenStart);
			//	break;

			// Do nothing if everything was okay.
			case Token.NULL:
				addNullToken();
				break;

			// All other token types don't continue to the next line...
			default:
				addToken(text, currentTokenStart,end-1, currentTokenType, newStartOffset+currentTokenStart);
				addNullToken();

		}

		// Return the first token in our linked list.
		return firstToken;

	}


}
