package in.shick.lockpatterngenerator;

import android.app.Activity;
import android.app.Dialog;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import android.widget.Button;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.util.Log;
import android.preference.PreferenceManager;
import java.util.LinkedList;
import java.util.Random;

public class GenLockPattern extends Activity
{
    public static final int DIALOG_LARGEGRID_ID = 0;
    private LockPatternView outputView;
    private SharedPreferences generationPrefs;
    private Random r;
    private int gridSize = 3;
    private int minNodes = 4;
    private int maxNodes = 6;
    private boolean highlightFirstNode = false;
    private boolean allowArbitraryGridSize = false;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        outputView = (LockPatternView) this.findViewById(R.id.patternOutputView);
        r = new Random();

        generationPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        updateSettings();

        final Button generateButton = (Button) this.findViewById(R.id.generateButton);
        final Button settingsButton = (Button) this.findViewById(R.id.settingsButton);
        final GenLockPattern toastAnchor = this;

        generateButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v)
            {
                toastAnchor.generateLockPattern();
            }
        });

        settingsButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v)
            {
                Intent walkIntoMordor = new Intent();
                walkIntoMordor.setClassName("in.shick.lockpatterngenerator", "in.shick.lockpatterngenerator.PreferencesActivity");
                startActivity(walkIntoMordor);
            }
        });
    }

    public void updateSettings()
    {
        int newGridSize, newMinNodes,  newMaxNodes;
        boolean clearPath = false;

        boolean newHighlightFirstNode = generationPrefs.getBoolean("firstNodePref",false);
        boolean newAllowArbitraryGridSize = generationPrefs.getBoolean("arbitraryGridPref",false);

        try
        {
            newGridSize = Integer.parseInt(generationPrefs.getString("gridSizePref","3").trim());
        }catch(java.lang.NumberFormatException e)
        {
            newGridSize = 3;
            generationPrefs.edit().putString("gridSizePref","3").commit();
        }

        try
        {
            newMinNodes = Integer.parseInt(generationPrefs.getString("minLengthPref","4").trim());
        }catch(java.lang.NumberFormatException e)
        {
            newMinNodes = 4;
            generationPrefs.edit().putString("minLengthPref","4").commit();
        }

        try
        {
            newMaxNodes = Integer.parseInt(generationPrefs.getString("maxLengthPref","6").trim());
        }catch(java.lang.NumberFormatException e)
        {
            newMaxNodes = 6;
            generationPrefs.edit().putString("maxLengthPref","6").commit();
        }


        allowArbitraryGridSize = newAllowArbitraryGridSize;
        if(!allowArbitraryGridSize)
        {
            if(newGridSize > 8)
            {
                newGridSize = 8;
                generationPrefs.edit().putString("gridSizePref","8").commit();
            }
        }


        if(newGridSize < 1)
        {
            newGridSize = 1;
            generationPrefs.edit().putString("gridSizePref","1").commit();
        }
        if(newMinNodes < 1)
        {
            newMinNodes = 1;
            generationPrefs.edit().putString("minLengthPref","1").commit();
        }
        if(newMinNodes > newGridSize*newGridSize)
        {
            newMinNodes = newGridSize*newGridSize;
            generationPrefs.edit().putString("minLengthPref",Integer.toString(newMinNodes)).commit();
        }

        if(newMaxNodes < 1)
        {
            newMaxNodes = 1;
            generationPrefs.edit().putString("maxLengthPref","1").commit();
        }
        if(newMaxNodes > newGridSize*newGridSize)
        {
            newMaxNodes = newGridSize*newGridSize;
            generationPrefs.edit().putString("maxLengthPref",Integer.toString(newMaxNodes)).commit();
        }

        if(newMinNodes > newMaxNodes)
        {
            newMaxNodes = newMinNodes;
            generationPrefs.edit().putString("maxLengthPref",Integer.toString(newMaxNodes)).commit();
        }

        if(newGridSize != gridSize || newMinNodes != minNodes || newMaxNodes != maxNodes)
            clearPath = true;

        highlightFirstNode = newHighlightFirstNode;
        gridSize = newGridSize;
        minNodes = newMinNodes;
        maxNodes = newMaxNodes;

        try
        {
            outputView.setGridSize(gridSize);
            outputView.setHighlight(highlightFirstNode);

            outputView.updateDrawableNodes();
        }catch(Throwable e)
        {
            if(gridSize <= 3)
                throw new java.lang.OutOfMemoryError();
            generationPrefs.edit().putString("gridSizePref","3").commit();
            showDialog(DIALOG_LARGEGRID_ID);
            updateSettings();
        }

        if(clearPath)
            outputView.updatePath(new LinkedList<Integer>());
    }

    public void onResume()
    {
        super.onResume();
        updateSettings();
        outputView.refreshPath();
        outputView.invalidate();
    }

    protected Dialog onCreateDialog(int id)
    {
        Dialog dialog = null;
        switch(id)
        {
            case DIALOG_LARGEGRID_ID:
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage("The device ran out of memory trying to render " +
                                   "the grid.  Try a smaller grid size.")
                       .setTitle("You broke it.")
                       .setCancelable(false)
                       .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                           public void onClick(DialogInterface dialog, int id) {
                               dialog.cancel();
                           }
                       });
                dialog = builder.create();
                break;
            default:
                dialog = null;
        }
        return dialog;
    }

    public void generateLockPattern()
    {
        /* path must implement java.util.Queue or else! */
        LinkedList<Integer> path = new LinkedList<Integer>();
        LinkedList<Integer> opts = new LinkedList<Integer>();
        int numNodes = r.nextInt(maxNodes-minNodes+1)+minNodes, currentNum = 0, rise = 0, run = 0, gcd = 0, lastNum = 0;

        for(int i = 0; i < gridSize*gridSize; i++)
            opts.offer(new Integer(i));

        currentNum = r.nextInt(gridSize*gridSize);
        path.offer(new Integer(currentNum));
        opts.remove(new Integer(currentNum));
        lastNum = currentNum;

        for(int i = 1; i < numNodes; i++)
        {
            if(opts.size() < 1)
                break;
            do
            {
                currentNum = opts.get(r.nextInt(opts.size())).intValue();
                rise = (currentNum / gridSize) - (lastNum / gridSize);
                run = (currentNum % gridSize) - (lastNum % gridSize);

                gcd = Math.abs(computeGcd(rise, run));

                if(gcd != 0)
                {
                    rise /= gcd;
                    run /= gcd;
                    for(int j = 1;j <= gcd; j++)
                    {
                        if(!path.contains(new Integer (lastNum + rise * j * gridSize + run * j)))
                        {
                            rise *= j;
                            run *= j;
                            break;
                        }
                    }
                }

                currentNum = lastNum + rise * gridSize + run;

            }while(path.contains(new Integer(currentNum)));

            path.offer(new Integer(currentNum));
            opts.remove(new Integer(currentNum));
            lastNum = currentNum;

        }

        outputView.updatePath(path);
        return;
    }

    private int computeGcd(int a, int b)
    /* Implementation taken from
     * http://en.literateprograms.org/Euclidean_algorithm_(Java)
     * Accessed on 12/28/10
     */
    {
        if(b > a)
        {
            int temp = a;
            a = b;
            b = temp;
        }

        while(b != 0)
        {
            int m = a % b;
            a = b;
            b = m;
        }

        return a;
    }
}