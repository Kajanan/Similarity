

package com.assign.chunk;

import com.assign.symbol.SymbolTableCompiler;


class OutcomeCounter {
    
    
    private final String mSymbol;

    private final SymbolTableCompiler mSymbolTable;

  
    private int mCount;

    
    private float mEstimate;

    public OutcomeCounter(String symbol, SymbolTableCompiler symbolTable, int count) {
        mSymbol = symbol;
        mSymbolTable = symbolTable;
        mCount = count;
    }


    public void increment() { 
    ++mCount; 
    }

    public int count() { 
    return mCount; 
    }

    public void addSymbolToTable() {
        if (mSymbol != null) 
        mSymbolTable.addSymbol(mSymbol);
    }

    public int getSymbolID() {
        return mSymbolTable.symbolToID(mSymbol);
    }

    public float estimate() { 
    return mEstimate; 
    }

    public void setEstimate(float estimate) { 
    mEstimate = estimate; 
    }

}
