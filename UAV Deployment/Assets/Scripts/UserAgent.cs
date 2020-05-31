using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using MLAgents;

public class UserAgent : Agent
{
    public GameObject ppoNoBSDrone;
    public GameObject heuresticDrone;
    public GameObject disMeanDrone;
    public GameObject[] humans = new GameObject[10];
    public float totalThroughput;
    public float disMeanThroughput;
    public float heuristicThroughput;
    public float preThroughput;
    public float initThroughput;
    public float Bandwidth;

    private bool isStop = false;

    public override void InitializeAgent()
    {
        //사람의 위치 및 각 demand 초기화
        for (int i = 0; i < 10; i++)
        {
            humans[i].transform.position = new Vector3(Random.Range(-250f, 250f), -7, Random.Range(-250f, 250f));
            if (Vector2.Distance(new Vector2(humans[i].transform.position.x, humans[i].transform.position.z), new Vector2(0, 0)) > 250f)
            {
                i--;
                continue;
            }
        }

        //드론의 위치 초기화
        gameObject.transform.position = new Vector3(-250, 5, -250);

        ComputeBandwidth(gameObject.transform.position);
        ComputeCapacity(gameObject.transform.position);
        ComputeThroughput();
        FinalTotalThroughput();

        initThroughput = totalThroughput;
        preThroughput = totalThroughput;

        CompareShceme();
    }

    public override void AgentReset()
    {
        //사람의 위치 및 각 demand 초기화
        for (int i = 0; i < 10; i++)
        {
            humans[i].transform.position = new Vector3(Random.Range(-250f, 250f), -7, Random.Range(-250f, 250f));
            if (Vector2.Distance(new Vector2(humans[i].transform.position.x, humans[i].transform.position.z), new Vector2(0, 0)) > 250f)
            {
                i--;
                continue;
            }
        }

        //드론의 위치 초기화
        gameObject.transform.position = new Vector3(-250, 5, -250);

        ComputeBandwidth(gameObject.transform.position);
        ComputeCapacity(gameObject.transform.position);
        ComputeThroughput();
        FinalTotalThroughput();

        initThroughput = totalThroughput;
        preThroughput = totalThroughput;

        CompareShceme();
    }

    public override void CollectObservations()
    {
        //Set State(위치, rates)
        AddVectorObs(gameObject.transform.position.x);
        AddVectorObs(gameObject.transform.position.z);
        for (int i = 0; i < 10; i++)
        {
            AddVectorObs(humans[i].transform.position.x);
            AddVectorObs(humans[i].transform.position.z);
            AddVectorObs(humans[i].GetComponent<HumanScripts>().demand);
        }
    }

    public override void AgentAction(float[] vectorAction, string textAction)
    {
        // Set Action.
        if(!isStop)
        {
            if (brain.brainParameters.vectorActionSpaceType == SpaceType.continuous)
            {
                var actionZ = 25f * Mathf.Clamp(vectorAction[0], -1f, 1f);
                var actionX = 25f * Mathf.Clamp(vectorAction[1], -1f, 1f);

                gameObject.transform.position = new Vector3(gameObject.transform.position.x + actionX, gameObject.transform.position.y, gameObject.transform.position.z + actionZ);
            }

            ComputeBandwidth(gameObject.transform.position);
            ComputeCapacity(gameObject.transform.position);
            ComputeThroughput();
            FinalTotalThroughput();
        }

        // Set Reward or Done.
        if(totalThroughput < initThroughput || gameObject.transform.position.x < -250f || gameObject.transform.position.z < -250f)
        {
            //SetReward(-5.0f);
            Done();
            //isStop = true;
        }
        else if(totalThroughput < preThroughput)
        {
            var reward = totalThroughput - initThroughput;
            //SetReward(reward);
            //if (preThroughput > disMeanThroughput)
            //{
            //    Debug.Log("11");
            //}
            //else
            //{
            //    Debug.Log("22");
            //}
            Done();
            //isStop = true;
        }
        else
        {
            var reward = totalThroughput - preThroughput;
            preThroughput = totalThroughput;
            //SetReward(reward);
        }
    }

    public void ComputeBandwidth(Vector3 _pos)
    {
        float B = 20;

        float Noise = 105;
        float distance = Vector3.Distance(new Vector3(_pos.x, 150, _pos.z), new Vector3(-250, 140, -250));
        float fspl = 20 * Mathf.Log((4 * 3.14f * distance * 1.5f) / (3 * Mathf.Pow(10, 8)), 10);
        float SNR = (-fspl + 10) / Noise;

        Bandwidth = B * Mathf.Log(1 + SNR, 2);
        if (Bandwidth >= 20f)
        {
            Bandwidth = 20f;
        }
        // Bandwidth = B;
    }
    public void ComputeCapacity(Vector3 _pos)
    {
        float Noise = 169;

        for (int i = 0; i < 10; ++i)
        {
            float distance = Vector3.Distance(new Vector3(_pos.x, 150, _pos.z), humans[i].transform.position);

            float seta = (180 / 3.14f) * Mathf.Atan(150 / Vector2.Distance(new Vector2(humans[i].transform.position.x, humans[i].transform.position.z), new Vector2(_pos.x, _pos.z)));

            float a = 4.88f;
            float b = 0.49f;
            float nLos = 0.1f;
            float nNLos = 21f;

            float pLos = 1 / (1 + a * Mathf.Exp(-b * (seta - a)));

            float fspl = 20 * Mathf.Log((4 * 3.14f * distance * 1.5f) / (3*Mathf.Pow(10,8)), 10) + pLos * nLos + (1 - pLos) * nNLos;
            float SNR = (-fspl + 10) / Noise;
            
            humans[i].GetComponent<HumanScripts>().capacity = Bandwidth * Mathf.Log(1 + SNR, 2);
        }
    }
    public void ComputeThroughput()
    {
        List<int> remainUsers = new List<int>();
        float timeFracAvail = 1f;
        for (int i = 0; i < 10; ++i)
        {
            humans[i].GetComponent<HumanScripts>().airtime = 0f;
            remainUsers.Add(i);
        }

        while (remainUsers.Count != 0 && timeFracAvail > 0.001f)
        {
            float timeFracPerUser = timeFracAvail / remainUsers.Count;
            for (int i = 0; i < remainUsers.Count; ++i)
            {
                int index = remainUsers[i];
                if ((humans[index].GetComponent<HumanScripts>().airtime + timeFracPerUser) * humans[index].GetComponent<HumanScripts>().capacity >= humans[index].GetComponent<HumanScripts>().demand)
                {
                    float fracUsed = (humans[index].GetComponent<HumanScripts>().demand / humans[index].GetComponent<HumanScripts>().capacity) - humans[index].GetComponent<HumanScripts>().airtime;
                    humans[index].GetComponent<HumanScripts>().airtime = humans[index].GetComponent<HumanScripts>().demand / humans[index].GetComponent<HumanScripts>().capacity;
                    timeFracAvail -= fracUsed;
                    remainUsers.RemoveAt(i);
                    i--;
                    continue;
                }
                else
                {
                    humans[index].GetComponent<HumanScripts>().airtime += timeFracPerUser;
                    timeFracAvail -= timeFracPerUser;
                }
            }
        }
    }
    public void FinalTotalThroughput()
    {
        totalThroughput = 0;
        for (int i = 0; i < 10; ++i)
        {
            humans[i].GetComponent<HumanScripts>().throughput = humans[i].GetComponent<HumanScripts>().airtime * humans[i].GetComponent<HumanScripts>().capacity;
            totalThroughput += humans[i].GetComponent<HumanScripts>().throughput;
        }
    }

    public void CompareShceme()
    {
        // Shortest Method
        Vector3 shortestPoint = new Vector3(0, 0, 0);
        for(int i = 0; i < 10; ++i)
        {
            shortestPoint.x += humans[i].gameObject.transform.position.x;
            shortestPoint.z += humans[i].gameObject.transform.position.z;
        }
        shortestPoint.x /= 10;
        shortestPoint.z /= 10;

        disMeanDrone.gameObject.transform.position = shortestPoint;
        ComputeBandwidth(disMeanDrone.gameObject.transform.position);
        ComputeCapacity(disMeanDrone.gameObject.transform.position);
        ComputeThroughput();
        FinalTotalThroughput();
        disMeanThroughput = totalThroughput;

        // Heurestic Method
        heuristicThroughput = 0.0f;
        for (int i = -150; i <= 150; ++i)
        {
            for (int j = -150; j <= 150; ++j)
            {
                ComputeBandwidth(new Vector3(shortestPoint.x + i, 10, shortestPoint.y + j));
                ComputeCapacity(new Vector3(shortestPoint.x + i, 10, shortestPoint.y + j));
                ComputeThroughput();
                FinalTotalThroughput();
                if (heuristicThroughput < totalThroughput)
                {
                    heuresticDrone.transform.position = new Vector3(shortestPoint.x + i, 10, shortestPoint.y + j);
                    heuristicThroughput = totalThroughput;
                }
            }
        }
    }
}
