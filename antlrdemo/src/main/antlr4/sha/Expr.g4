grammar Expr;
// at the top level its always a struct
expr:	struct;

struct: 'struct' '<' term (',' term)* '>'
        | 'struct' '<' '>';

term:	WORD ':' type
    ;

WORD :    '`'[a-zA-Z0-9_/\-]+'`';

type :    'timestamp'
     |    'boolean'
     |    'bigint'
     |    'string'
     |    'double'
     |    'array' '<' type '>'
     |     struct;

WS  :     [ \t\n\r]+ -> skip ;
