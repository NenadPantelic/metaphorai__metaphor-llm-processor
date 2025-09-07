Please, find all direct/indirect/implicit metaphors in the text below strictly adhering to  MIPVU  (Steen et al. 2010) methodology. Please, use the following rules:
1. Read the entire text/discourse to establish a general understanding of meaning.
2. Determine the lexical units in the text/discourse (noting exceptions to certain phrasal verbs, conventionalized compounds, and polywords)-.
3. For each lexical unit in the text, establish its contextual meaning.
4. For each lexical unit, determine if it has a more basic contemporary meaning in other contexts than the one in the given context.
5. If it has a more basic current meaning in other contexts than the given context, decide whether the contextual meaning contrasts with the basic meaning but can be understood in comparison with it. If yes, mark the lexical unit as metaphorical (MRW).
6. To establish the basic meaning of a word, use https://www.ldoceonline.com/ and https://dictionary.cambridge.org/ . To establish the contextual meaning of a word, use  https://www.ldoceonline.com/ and https://dictionary.cambridge.org/
7. The basic meaning must always be: more concrete, clearly connected to bodily experiences, human-oriented and directly imaginable (physical sensations, actions, perceptions, tangible objects, or concrete situations).
8. Provide an explanation for all words you have identified as a direct/indirect/implicit metaphor.
9. You should return the results in JSON format (a list of words, i.e. jsons) using the following structure:
    - String phrase - found metaphor
    - int offset - the offset of the metaphor in a given text (e.g. if in a sentence of 10 words, the metaphor starts at third word
    the offset should be the number of characters before it reaches the third word)
    - String metaphorType - DIRECT/INDIRECT/IMPLICIT (use upper case values!)
    - String explanation - explain how this phrase is metaphor

Text that should be analyzed:
{text}
